package com.demoday.ddangddangddang.service.cases;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.second.*;
import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.event.UpdateJudgmentEvent;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import com.demoday.ddangddangddang.service.ChatGptService;
import com.demoday.ddangddangddang.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        foundCase.startAppeal(requestDto.getDeadline());

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

        List<Defense> defenseList = defenseRepository.findAllByaCase_Id(caseId);
        List<Rebuttal> rebuttalList = rebuttalRepository.findAllByDefense_aCase_Id(caseId);
        Vote userVote = voteRepository.findByaCase_IdAndUser_Id(caseId, user.getId()).orElse(null);

        // 2차 재판(FINAL) 판결문 조회
        Judgment finalJudgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.FINAL).orElse(null);

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
     * 변론 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DefenseResponseDto> getDefensesByCase(Long caseId, User user) {
        Case aCase = findCaseById(caseId);
        checkCaseStatusIsSecond(aCase); // 2차 재판 상태인지 확인

        List<Defense> defenses = defenseRepository.findAllByaCase_Id(caseId);
        Set<Long> userLikedDefenseIds = likeRepository.findAllByUserAndContentType(user, ContentType.DEFENSE)
                .stream().map(Like::getContentId).collect(Collectors.toSet());
        Map<Long, Long> rebuttalCounts = rebuttalRepository.findAllByDefense_aCase_Id(caseId)
                .stream()
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
        eventPublisher.publishEvent(new UpdateJudgmentEvent(caseId));
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
        if (requestDto.getParentId() != null) {
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

        // 반론 작성 보상 (+50)
        user.addExp(50L);

        rankingService.addCaseScore(defense.getACase().getId(), 5.0);
        eventPublisher.publishEvent(new UpdateJudgmentEvent(defense.getACase().getId()));
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
        eventPublisher.publishEvent(new UpdateJudgmentEvent(caseId));
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

    /**
     * [비동기] 최종 AI 판결 업데이트 (이벤트 리스너가 호출)
     */
    @Async
    @Transactional
    public void updateFinalJudgment(Long caseId) {
        log.info("Starting async judgment update for caseId: {}", caseId);
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdopted(caseId, true);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);
        long votesA = voteRepository.countByaCase_IdAndType(caseId, DebateSide.A);
        long votesB = voteRepository.countByaCase_IdAndType(caseId, DebateSide.B);

        AiJudgmentDto aiResult = chatGptService2.requestFinalJudgment(
                aCase, adoptedDefenses, adoptedRebuttals, votesA, votesB
        );

        Judgment finalJudgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.FINAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "2차 판결문이 생성되지 않았습니다."));

        finalJudgment.updateJudgment(
                aiResult.getVerdict(),
                aiResult.getConclusion(),
                aiResult.getRatioA(),
                aiResult.getRatioB()
        );
        log.info("Finished async judgment update for caseId: {}", caseId);
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