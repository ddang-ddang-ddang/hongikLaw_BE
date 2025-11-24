package com.demoday.ddangddangddang.service.cases;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
import com.demoday.ddangddangddang.domain.event.PostCreatedEvent;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.second.*;
import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import com.demoday.ddangddangddang.dto.notice.NotificationResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptableItemDto;
import com.demoday.ddangddangddang.dto.third.JudgementBasisDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.event.UpdateJudgmentEvent;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.*;
import com.demoday.ddangddangddang.service.ChatGptService;
import com.demoday.ddangddangddang.service.ExpService;
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
import java.util.Collections;

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
    private final UserRepository userRepository;
    private final ExpService expService;

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

        // 1차 판결문의 텍스트(basedOn)를 그대로 복사하지 않고, 빈 JSON 구조를 넣어줍니다.
        JudgementBasisDto emptyBasis = new JudgementBasisDto(Collections.emptyList(), Collections.emptyList());
        String emptyBasisJson;
        try {
            emptyBasisJson = objectMapper.writeValueAsString(emptyBasis);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("초기 설정 중 JSON 변환 오류", e);
        }

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
     * (비로그인 유저 가능, 최종심/종료된 사건도 조회 가능)
     */
    @Transactional(readOnly = true)
    public CaseDetail2ndResponseDto getDebateDetails(Long caseId, User user) {
        Case aCase = findCaseById(caseId);

        // [수정] PENDING 또는 FIRST 상태는 비공개 (조회 불가)
        if (aCase.getStatus() == CaseStatus.PENDING || aCase.getStatus() == CaseStatus.FIRST) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "공개되지 않은 재판이거나 권한이 없습니다.");
        }
        // (참고) SOLO 모드에서 2차를 안 가고 DONE이 된 경우도 appealDeadline이 null이므로 아래 로직 등으로 걸러낼 수 있으나,
        // 현재 DONE 상태는 getFinishedCases에서 필터링하고, 여기서는 ID를 알고 들어온 경우 보여줄지 말지 결정.
        // 기획상 '1차에서 끝낸 것(FIRST/DONE(solo))'은 비공개이므로 appealDeadline 체크 추가 가능.
        if (aCase.getStatus() == CaseStatus.DONE && aCase.getAppealDeadline() == null) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "비공개 종료된 사건입니다.");
        }

        // 1. 변론 조회 (BLIND 제외)
        List<Defense> defenseList = defenseRepository.findAllByaCase_IdAndIsBlindFalse(caseId);

        // 2. 반론 조회 (BLIND 제외)
        List<Rebuttal> rebuttalList = rebuttalRepository.findAllByDefense_aCase_Id(caseId).stream()
                .filter(r -> !r.getIsBlind())
                .collect(Collectors.toList());

        // 3. 유저 관련 정보 (Vote, Like) - 비로그인(Guest) 처리
        Vote userVote = null;
        Set<Long> userLikedDefenseIds = Set.of();
        Set<Long> userLikedRebuttalIds = Set.of();

        if (user != null) {
            userVote = voteRepository.findByaCase_IdAndUser_Id(caseId, user.getId()).orElse(null);
            userLikedDefenseIds = likeRepository.findAllByUserAndContentType(user, ContentType.DEFENSE)
                    .stream().map(Like::getContentId).collect(Collectors.toSet());
            userLikedRebuttalIds = likeRepository.findAllByUserAndContentType(user, ContentType.REBUTTAL)
                    .stream().map(Like::getContentId).collect(Collectors.toSet());
        }

        // 4. 판결문 조회 (최종심 우선, 없으면 초심)
        Judgment finalJudgment = judgmentRepository.findTopByaCase_IdAndStageOrderByCreatedAtDesc(caseId, JudgmentStage.FINAL)
                .orElse(judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.INITIAL).orElse(null));

        // 5. 1차 입장문 조회 및 매핑
        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        ArgumentInitial argA = arguments.stream().filter(a -> a.getType() == DebateSide.A).findFirst().orElse(null);
        ArgumentInitial argB = arguments.stream().filter(a -> a.getType() == DebateSide.B).findFirst().orElse(null);

        return CaseDetail2ndResponseDto.fromEntities(
                aCase, defenseList, rebuttalList, userVote, finalJudgment,
                userLikedDefenseIds, userLikedRebuttalIds,
                argA, argB
        );
    }

    /**
     * [신규] 최종 판결 이후의 사건 목록 조회 (THIRD, DONE)
     */
    @Transactional(readOnly = true)
    public List<CaseOnResponseDto> getFinishedCases() {
        // 1. THIRD 상태 (최종심 진행 중, 이미 2차는 끝남)
        List<Case> thirdCases = caseRepository.findAllByStatusOrderByCreatedAtDesc(CaseStatus.THIRD);

        // 2. DONE 상태 중 AppealDeadline이 있는 것 (2차를 거쳐 종료된 것)
        List<Case> doneCases = caseRepository.findAllByStatusAndAppealDeadlineIsNotNullOrderByCreatedAtDesc(CaseStatus.DONE);

        // 합치고 최신순 정렬
        List<Case> allFinishedCases = Stream.concat(thirdCases.stream(), doneCases.stream())
                .sorted(Comparator.comparing(Case::getCreatedAt).reversed())
                .collect(Collectors.toList());

        return convertToDto(allFinishedCases);
    }

    /**
     * 2차 재판 진행 목록 조회 (SECOND 상태)
     */
    @Transactional(readOnly = true)
    public List<CaseOnResponseDto> getSecondStageCases() {
        // SECOND 상태인 사건만 조회
        List<Case> secondCases = caseRepository.findAllByStatusOrderByCreatedAtDesc(CaseStatus.SECOND);
        return convertToDto(secondCases);
    }

    /**
     * 변론 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DefenseResponseDto> getDefensesByCase(Long caseId, User user) {
        Case aCase = findCaseById(caseId);

        // [수정] 상태 체크 로직 (getDebateDetails와 동일한 기준으로 조회 허용)
        if (aCase.getStatus() == CaseStatus.PENDING || aCase.getStatus() == CaseStatus.FIRST) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "공개되지 않은 재판입니다.");
        }

        List<Defense> defenses = defenseRepository.findAllByaCase_IdAndIsBlindFalse(caseId);

        Set<Long> userLikedDefenseIds = (user != null) ?
                likeRepository.findAllByUserAndContentType(user, ContentType.DEFENSE)
                        .stream().map(Like::getContentId).collect(Collectors.toSet())
                : Set.of();

        Map<Long, Long> rebuttalCounts = rebuttalRepository.findAllByDefense_aCase_Id(caseId)
                .stream()
                .filter(r -> !r.getIsBlind())
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
     * 반론 목록 조회 (수정됨)
     */
    @Transactional(readOnly = true)
    public List<RebuttalResponseDto> getRebuttalsByDefense(Long defenseId, User user) {
        // 1. 블라인드 처리된 반론 제외
        List<Rebuttal> rebuttals = rebuttalRepository.findAllByDefense_Id(defenseId).stream()
                .filter(r -> !r.getIsBlind())
                .collect(Collectors.toList());

        // 2. 좋아요 여부 확인 (비로그인 유저 null 체크 추가)
        Set<Long> userLikedRebuttalIds = (user != null) ?
                likeRepository.findAllByUserAndContentType(user, ContentType.REBUTTAL)
                        .stream().map(Like::getContentId).collect(Collectors.toSet())
                : Set.of();

        return RebuttalResponseDto.buildTree(rebuttals, userLikedRebuttalIds);
    }

    /**
     * 변론 제출
     */
    @Transactional
    public Defense createDefense(Long caseId, DefenseRequestDto requestDto, User user) {
        Case aCase = findCaseById(caseId);

        // [수정] 작성 가능 상태 체크 (SECOND, THIRD, DONE 모두 가능)
        checkCaseStatusForDebate(aCase);

        // User를 영속 상태로 조회 (Reload)
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Defense defense = Defense.builder()
                .aCase(aCase)
                .user(managedUser)
                .type(requestDto.getSide())
                .content(requestDto.getContent())
                .caseResult(CaseResult.ONGOING)
                .build();
        Defense savedDefense = defenseRepository.save(defense);

        expService.addExp(managedUser, 50L, "변론 작성");
        rankingService.addCaseScore(caseId, 5.0);
        eventPublisher.publishEvent(new PostCreatedEvent(user, ContentType.DEFENSE));
        return savedDefense;
    }

    /**
     * 반론(대댓글) 제출
     */
    @Transactional
    public Rebuttal createRebuttal(RebuttalRequestDto requestDto, User user) {
        Defense defense = defenseRepository.findById(requestDto.getDefenseId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "원본 변론을 찾을 수 없습니다."));

        // [수정] 작성 가능 상태 체크
        checkCaseStatusForDebate(defense.getACase());

        // User를 영속 상태로 조회
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Rebuttal parentRebuttal = null;
        if (requestDto.getParentId() != null && requestDto.getParentId() != 0L) {
            parentRebuttal = rebuttalRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "부모 반론을 찾을 수 없습니다."));
            if (!parentRebuttal.getDefense().getId().equals(defense.getId())) {
                throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 부모 반론 ID 입니다.");
            }
        }

        Rebuttal rebuttal = Rebuttal.builder()
                .defense(defense)
                .user(managedUser)
                .parent(parentRebuttal)
                .type(requestDto.getType())
                .content(requestDto.getContent())
                .caseResult(CaseResult.ONGOING)
                .build();
        Rebuttal savedRebuttal = rebuttalRepository.save(rebuttal);

        // 알림 로직 (기존 유지)
        sendRebuttalNotification(rebuttal, defense, parentRebuttal, user);

        expService.addExp(managedUser, 50L, "반론 작성");
        rankingService.addCaseScore(defense.getACase().getId(), 5.0);
        eventPublisher.publishEvent(new PostCreatedEvent(user, ContentType.REBUTTAL));
        return savedRebuttal;
    }

    /**
     * 배심원 투표
     */
    @Transactional
    public void castVote(Long caseId, VoteRequestDto requestDto, User user) {
        Case aCase = findCaseById(caseId);

        // [중요] 투표는 SECOND 상태에서만 가능 (기존 로직 유지 + STRICT Check)
        if (aCase.getStatus() != CaseStatus.SECOND) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "투표가 마감되었거나 진행 중이 아닙니다.");
        }
        checkDeadline(aCase); // 마감 시간 이중 체크

        // User를 영속 상태로 조회
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Vote vote = voteRepository.findByaCase_IdAndUser_Id(caseId, user.getId())
                .map(existingVote -> {
                    existingVote.updateChoice(requestDto.getChoice());
                    return existingVote;
                })
                .orElseGet(() -> {
                    // [수정 후] 첫 투표일 때만 지급
                    expService.addExp(managedUser, 10L, "투표 참여");
                    return Vote.builder()
                            .aCase(aCase)
                            .user(user)
                            .type(requestDto.getChoice())
                            .build();
                });
        voteRepository.save(vote);
        rankingService.addCaseScore(caseId, 3.0);
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

    // [수정] 변론/반론 작성 시 상태 체크 (SECOND, THIRD, DONE 허용, 단 1차 종료는 제외)
    private void checkCaseStatusForDebate(Case aCase) {
        if (aCase.getStatus() == CaseStatus.PENDING || aCase.getStatus() == CaseStatus.FIRST) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "토론이 활성화되지 않은 사건입니다.");
        }
        // DONE 상태여도 appealDeadline이 없으면 1차에서 끝난 사건(비공개)이므로 작성 불가
        if (aCase.getStatus() == CaseStatus.DONE && aCase.getAppealDeadline() == null) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "종료된 비공개 사건입니다.");
        }
    }

    private void checkDeadline(Case aCase) {
        if (aCase.getAppealDeadline() != null && aCase.getAppealDeadline().isBefore(LocalDateTime.now())) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "투표가 마감되었습니다.");
        }
    }

    private void sendRebuttalNotification(Rebuttal rebuttal, Defense defense, Rebuttal parentRebuttal, User user) {
        // ... 기존 알림 로직 ...
        Long targetUserId = (parentRebuttal != null) ? parentRebuttal.getUser().getId() : defense.getUser().getId();
        if (!targetUserId.equals(user.getId())) {
            NotificationResponseDto dto = NotificationResponseDto.builder()
                    .message(parentRebuttal != null ? "내 반론에 대댓글이 달렸습니다." : "내 변론에 반론이 달렸습니다.")
                    .caseId(defense.getACase().getId())
                    .defenseId(defense.getId())
                    .parentId(parentRebuttal != null ? parentRebuttal.getId() : null)
                    .rebuttalId(rebuttal.getId())
                    .iconUrl("https://ddangddangddang-demoday.s3.ap-northeast-2.amazonaws.com/icons/versus.png")
                    .build();
            sseEmitters.sendNotification(targetUserId, "notification", dto);
        }
    }

    // DTO 변환 헬퍼 (중복 코드 제거용)
    private List<CaseOnResponseDto> convertToDto(List<Case> cases) {
        if (cases.isEmpty()) return List.of();

        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseInOrderByTypeAsc(cases);
        Map<Long, List<String>> argumentsMap = arguments.stream()
                .collect(Collectors.groupingBy(
                        argument -> argument.getACase().getId(),
                        Collectors.mapping(ArgumentInitial::getMainArgument, Collectors.toList())
                ));

        return cases.stream()
                .map(aCase -> CaseOnResponseDto.builder()
                        .caseId(aCase.getId())
                        .title(aCase.getTitle())
                        .status(aCase.getStatus())
                        .mainArguments(argumentsMap.getOrDefault(aCase.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }
}