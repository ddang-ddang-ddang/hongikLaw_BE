package com.demoday.ddangddangddang.service.cases;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
import com.demoday.ddangddangddang.domain.event.PostCreatedEvent;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.second.*;
import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptableItemDto;
import com.demoday.ddangddangddang.dto.third.JudgementBasisDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.event.UpdateJudgmentEvent;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.*;
import com.demoday.ddangddangddang.service.ChatGptService;
import com.demoday.ddangddangddang.service.ranking.RankingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.demoday.ddangddangddang.dto.home.CaseOnResponseDto;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebateService {

    private final CaseRepository caseRepository;
    private final JudgmentRepository judgmentRepository;
    private final DefenseRepository defenseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final VoteRepository voteRepository;
    private final LikeRepository likeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RankingService rankingService;
    private final ChatGptService chatGptService2;
    private final ObjectMapper objectMapper;
    private final SseEmitters sseEmitters;
    private final ArgumentInitialRepository argumentInitialRepository;

    /**
     * 2차 재판 시작
     */
    @Transactional
    public void startAppeal(Long caseId, AppealRequestDto requestDto, User user) {
        Case foundCase = findCaseById(caseId);
        // TODO: 본인 확인 로직

        // 이미 2차 재판이 시작되었거나 종료된 사건에 대한 요청 방지
        if (foundCase.getStatus() != CaseStatus.FIRST) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "이미 2차 재판이 시작되었거나 종료된 사건입니다.");
        }

        Integer hours = requestDto.getHoursToAdd();
        LocalDateTime deadline = LocalDateTime.now().plusHours(hours);

        foundCase.startAppeal(deadline);

        // 2차 재판 시작 시, 1차 판결문 기반으로 '최종심' 판결문 생성 (이후 계속 업데이트됨)
        Judgment initialJudgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.INITIAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "1차 판결문이 없습니다."));

        Judgment finalJudgment = Judgment.builder()
                .aCase(foundCase)
                .stage(JudgmentStage.FINAL)
                .content(initialJudgment.getContent()) // 1차 내용 복사
                .basedOn(initialJudgment.getBasedOn()) // 1차 내용 복사
                .ratioA(initialJudgment.getRatioA())
                .ratioB(initialJudgment.getRatioB())
                .build();
        judgmentRepository.save(finalJudgment);
    }

    /**
     * 2차 재판 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public CaseDetail2ndResponseDto getDebateDetails(Long caseId, User user) {
        Case aCase = findCaseById(caseId);
        checkCaseStatusIsSecond(aCase); // 2차 재판 상태인지 확인

        // 수정: isBlind=false 조건 추가
        List<Defense> defenseList = defenseRepository.findAllByaCase_IdAndIsBlindFalse(caseId); // (수정된 메서드 사용 가정)
        // 수정: Rebuttal 조회에도 isBlind=false 조건이 포함되어야 합니다. (QueryDSL 사용 시 용이)
        // JPA 메서드명은 복잡해지므로, Repositroy에 메서드 추가가 필요합니다. (여기서는 간단한 예시로 대체)
        List<Rebuttal> rebuttalList = rebuttalRepository.findAllByDefense_aCase_Id(caseId).stream()
                .filter(r -> !r.getIsBlind())
                .collect(Collectors.toList());

        Vote userVote = voteRepository.findByaCase_IdAndUser_Id(caseId, user.getId()).orElse(null);

        // 2차 재판(FINAL) 판결문 조회 (가장 최신 버전으로 수정)
        Judgment finalJudgment = judgmentRepository.findTopByaCase_IdAndStageOrderByCreatedAtDesc(caseId, JudgmentStage.FINAL).orElse(null);

        // 사용자가 좋아요 누른 변론/반론 ID 목록 조회
        Set<Long> userLikedDefenseIds = likeRepository.findAllByUserAndContentType(user, ContentType.DEFENSE)
                .stream().map(Like::getContentId).collect(Collectors.toSet());
        Set<Long> userLikedRebuttalIds = likeRepository.findAllByUserAndContentType(user, ContentType.REBUTTAL)
                .stream().map(Like::getContentId).collect(Collectors.toSet());

        return CaseDetail2ndResponseDto.fromEntities(
                aCase, defenseList, rebuttalList, userVote, finalJudgment, userLikedDefenseIds, userLikedRebuttalIds
        );
    }

    /**
     * 2차 재판 진행 목록 조회 (SECOND 상태)
     */
    @Transactional(readOnly = true)
    public List<CaseOnResponseDto> getSecondStageCases() {
        // 1. SECOND 상태인 사건 목록을 최신순으로 조회
        List<Case> secondCases = caseRepository.findAllByStatusOrderByCreatedAtDesc(CaseStatus.SECOND);

        if (secondCases.isEmpty()) {
            return List.of();
        }

        // 2. N+1 문제 방지를 위해 모든 SECOND 사건의 1차 입장문을 한 번에 조회
        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseInOrderByTypeAsc(secondCases);

        // 3. Case ID 기준으로 A, B측 입장문(mainArgument) 목록을 매핑
        Map<Long, List<String>> argumentsMap = arguments.stream()
                .collect(Collectors.groupingBy(
                        argument -> argument.getACase().getId(),
                        Collectors.mapping(ArgumentInitial::getMainArgument, Collectors.toList())
                ));

        // 4. DTO로 변환하여 반환
        return secondCases.stream()
                .map(aCase -> CaseOnResponseDto.builder()
                        .caseId(aCase.getId())
                        .title(aCase.getTitle())
                        .status(aCase.getStatus())
                        .mainArguments(argumentsMap.getOrDefault(aCase.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 변론 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DefenseResponseDto> getDefensesByCase(Long caseId, User user) {
        Case aCase = findCaseById(caseId);
        checkCaseStatusIsSecond(aCase); // 2차 재판 상태인지 확인

        // 1. [수정] DefenseRepository의 새로운 메서드를 사용하여 BLIND 처리되지 않은 변론만 조회합니다.
        List<Defense> defenses = defenseRepository.findAllByaCase_IdAndIsBlindFalse(caseId);

        Set<Long> userLikedDefenseIds = likeRepository.findAllByUserAndContentType(user, ContentType.DEFENSE)
                .stream().map(Like::getContentId).collect(Collectors.toSet());

        // 2. [수정] 반론 개수를 셀 때, 반론 엔티티의 isBlind 필드가 false인 항목만 카운트하도록 필터링을 추가합니다.
        Map<Long, Long> rebuttalCounts = rebuttalRepository.findAllByDefense_aCase_Id(caseId)
                .stream()
                .filter(r -> !r.getIsBlind()) // ✨ BLIND 반론 제외 필터링 추가
                .collect(Collectors.groupingBy(r -> r.getDefense().getId(), Collectors.counting()));

        return defenses.stream()
                .map(defense -> DefenseResponseDto.fromEntity(
                        defense,
                        userLikedDefenseIds.contains(defense.getId()),
                        rebuttalCounts.getOrDefault(defense.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    /**
     * 반론 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RebuttalResponseDto> getRebuttalsByDefense(Long defenseId, User user) {
        List<Rebuttal> rebuttals = rebuttalRepository.findAllByDefense_Id(defenseId);
        Set<Long> userLikedRebuttalIds = likeRepository.findAllByUserAndContentType(user, ContentType.REBUTTAL)
                .stream().map(Like::getContentId).collect(Collectors.toSet());
        return RebuttalResponseDto.buildTree(rebuttals, userLikedRebuttalIds);
    }

    /**
     * 변론 제출
     */
    @Transactional
    public Defense createDefense(Long caseId, DefenseRequestDto requestDto, User user) {
        Case aCase = findCaseById(caseId);
        checkCaseStatusIsSecond(aCase); // 2차 재판 상태인지 확인

        Defense defense = Defense.builder()
                .aCase(aCase)
                .user(user)
                .type(requestDto.getSide())
                .content(requestDto.getContent())
                .caseResult(CaseResult.PENDING)
                .build();
        Defense savedDefense = defenseRepository.save(defense);

        // 변론 작성 보상 (+50)
        user.addExp(50L);

        rankingService.addCaseScore(caseId, 5.0);

        eventPublisher.publishEvent(new PostCreatedEvent(user,ContentType.DEFENSE));
        // eventPublisher.publishEvent(new UpdateJudgmentEvent(caseId)); // 실시간 ai 판결 업데이트 임시 주석
        return savedDefense;
    }

    /**
     * 반론(대댓글) 제출
     */
    @Transactional
    public Rebuttal createRebuttal(RebuttalRequestDto requestDto, User user) {
        Defense defense = defenseRepository.findById(requestDto.getDefenseId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "원본 변론을 찾을 수 없습니다."));
        checkCaseStatusIsSecond(defense.getACase());

        Rebuttal parentRebuttal = null;

        // parentId가 0 또는 null이 아닐 때만 부모 찾도록 수정
        if (requestDto.getParentId() != null && requestDto.getParentId() != 0L) {
            parentRebuttal = rebuttalRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "부모 반론을 찾을 수 없습니다."));

            if (!parentRebuttal.getDefense().getId().equals(defense.getId())) {
                throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 부모 반론 ID 입니다. 해당 변론에 속하지 않습니다.");
            }
        }

        Rebuttal rebuttal = Rebuttal.builder()
                .defense(defense)
                .user(user)
                .parent(parentRebuttal)
                .type(requestDto.getType())
                .content(requestDto.getContent())
                .caseResult(CaseResult.PENDING)
                .build();
        Rebuttal savedRebuttal = rebuttalRepository.save(rebuttal);

        // [알림 로직]
        // 1. 대대댓글(답글의 답글)인 경우 -> 바로 위 부모 댓글 작성자에게 알림
        if (parentRebuttal != null) {
            Long targetUserId = parentRebuttal.getUser().getId();
            // 본인이 본인 글에 단 경우는 알림 제외
            if (!targetUserId.equals(user.getId())) {
                sseEmitters.sendNotification(targetUserId, "notification", "내 반론에 새로운 대댓글이 달렸습니다.");
            }
        }
        // 2. 일반 반론(댓글)인 경우 -> 변론(게시글) 작성자에게 알림
        else {
            Long targetUserId = defense.getUser().getId();
            // 본인이 본인 변론에 댓글 단 경우 제외
            if (!targetUserId.equals(user.getId())) {
                sseEmitters.sendNotification(targetUserId, "notification", "내 변론에 새로운 반론이 달렸습니다.");
            }
        }

        // 반론 작성 보상 (+50)
        user.addExp(50L);

        rankingService.addCaseScore(defense.getACase().getId(), 5.0);

        eventPublisher.publishEvent(new PostCreatedEvent(user,ContentType.REBUTTAL));
        // eventPublisher.publishEvent(new UpdateJudgmentEvent(defense.getACase().getId())); // 실시간 ai 판결 업데이트 임시 주석
        return savedRebuttal;
    }

    /**
     * 배심원 투표
     */
    @Transactional
    public void castVote(Long caseId, VoteRequestDto requestDto, User user) {
        Case aCase = findCaseById(caseId);
        checkCaseStatusIsSecond(aCase);

        checkDeadline(aCase);

        Vote vote = voteRepository.findByaCase_IdAndUser_Id(caseId, user.getId())
                .map(existingVote -> {
                    // 이미 투표했으면 경험치 추가 지급 안함 (입장 변경만)
                    existingVote.updateChoice(requestDto.getChoice());
                    return existingVote;
                })
                .orElseGet(() -> {
                    // 첫 투표일 경우에만 경험치 지급
                    user.addExp(10L); // 투표 보상 (+10)
                    return Vote.builder()
                            .aCase(aCase)
                            .user(user)
                            .type(requestDto.getChoice())
                            .build();
                });
        voteRepository.save(vote);

        rankingService.addCaseScore(caseId, 3.0);
        // eventPublisher.publishEvent(new UpdateJudgmentEvent(caseId)); // 실시간 ai 판결 업뎃 주석
    }

    /**
     * 투표 결과 조회
     */
    @Transactional(readOnly = true)
    public VoteResultResponseDto getVoteResult(Long caseId, User user) {
        long aCount = voteRepository.countByaCase_IdAndType(caseId, DebateSide.A);
        long bCount = voteRepository.countByaCase_IdAndType(caseId, DebateSide.B);
        long total = aCount + bCount;

        double aPercent = total == 0 ? 0 : (aCount * 100.0 / total);
        double bPercent = total == 0 ? 0 : (bCount * 100.0 / total);

        return VoteResultResponseDto.builder()
                .totalVotes(total)
                .aCount(aCount)
                .bCount(bCount)
                .aPercent(aPercent)
                .bPercent(bPercent)
                .build();
    }

    // --- 공통 헬퍼 메서드 ---

    private Case findCaseById(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "사건을 찾을 수 없습니다."));
    }

    private void checkCaseStatusIsSecond(Case aCase) {
        if (aCase.getStatus() != CaseStatus.SECOND) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "현재 2차 재판(변론/투표)이 진행 중인 사건이 아닙니다.");
        }
    }

    // 투표 마감 시간 체크
    private void checkDeadline(Case aCase) {
        if (aCase.getAppealDeadline() != null && aCase.getAppealDeadline().isBefore(LocalDateTime.now())) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "투표가 마감되었습니다.");
        }
    }
}