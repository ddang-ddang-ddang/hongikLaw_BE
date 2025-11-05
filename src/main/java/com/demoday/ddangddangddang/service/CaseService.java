package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.dto.caseDto.second.*;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.event.UpdateJudgmentEvent; // [추가] 이벤트
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import com.demoday.ddangddangddang.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가] log 사용을 위해
import org.springframework.context.ApplicationEventPublisher; // [추가] 이벤트 발행
import org.springframework.scheduling.annotation.Async; // [추가] 비동기 처리를 위해
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final JudgmentRepository judgmentRepository;
    private final ChatGptService2 chatGptService2;
    private final DefenseRepository defenseRepository; // [추가] 2차 재판용
    private final RebuttalRepository rebuttalRepository; // [추가] 2차 재판용
    private final VoteRepository voteRepository; // [추가] 2차 재판용
    private final LikeRepository likeRepository; // [추가] 2차 재판용
    private final ApplicationEventPublisher eventPublisher; // [추가] 이벤트 발행기
    private final RankingService rankingService;

    // --- [ 1차 재판(초심) 관련 메서드 ] ---

    @Transactional
    public CaseResponseDto createCase(CaseRequestDto requestDto, User user) {
        if (!requestDto.getMode().equals(CaseMode.SOLO)) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "현재 솔로 모드만 지원합니다.");
        }

        // 1. 사건(Case) 생성 및 저장
        Case newCase = Case.builder()
                .mode(requestDto.getMode())
                .title(requestDto.getTitle())
                .status(CaseStatus.FIRST)
                .build();
        caseRepository.save(newCase);

        // 2. A측 1차 입장문 생성 및 저장
        ArgumentInitial argumentA = ArgumentInitial.builder()
                .aCase(newCase)
                .user(user)
                .type(DebateSide.A)
                .mainArgument(requestDto.getArgumentAMain())
                .reasoning(requestDto.getArgumentAReasoning())
                .build();
        argumentInitialRepository.save(argumentA);

        // 3. B측 1차 입장문 생성 및 저장
        ArgumentInitial argumentB = ArgumentInitial.builder()
                .aCase(newCase)
                .user(user)
                .type(DebateSide.B)
                .mainArgument(requestDto.getArgumentBMain())
                .reasoning(requestDto.getArgumentBReasoning())
                .build();
        argumentInitialRepository.save(argumentB);

        // --- [ 4. AI 판결 로직 ] ---
        // TODO: AI 호출이 오래 걸릴 수 있으므로, @Async 등을 이용한 비동기 처리로 리팩토링 권장
        // 현재는 1차 재판 생성 시 바로 AI 판결을 받으므로 동기 처리
        List<ArgumentInitial> arguments = List.of(argumentA, argumentB);
        AiJudgmentDto aiResult = chatGptService2.getAiJudgment(newCase, arguments);

        // 5. AI의 판결 결과를 DB에 저장
        Judgment judgment = Judgment.builder()
                .aCase(newCase)
                .stage(JudgmentStage.INITIAL)
                .content(aiResult.getVerdict()) // AI가 생성한 판결 내용
                .basedOn(aiResult.getConclusion()) // AI가 생성한 결론
                .ratioA(aiResult.getRatioA()) // AI가 생성한 비율
                .ratioB(aiResult.getRatioB()) // AI가 생성한 비율
                .build();
        judgmentRepository.save(judgment);

        return new CaseResponseDto(newCase.getId());
    }

    @Transactional(readOnly = true)
    public JudgmentResponseDto getJudgment(Long caseId, User user) {
        Case foundCase = findCaseById(caseId); // 헬퍼 메서드 사용

        // TODO: 본인 확인 로직 (솔로 모드이므로 본인이 생성한 사건인지 확인)

        // 초심(INITIAL) 판결문 조회
        Judgment judgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.INITIAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "아직 판결이 완료되지 않았습니다."));

        return new JudgmentResponseDto(judgment);
    }

    @Transactional(readOnly = true)
    public CaseDetailResponseDto getCaseDetail(Long caseId, User user) {
        Case aCase = findCaseById(caseId); // 헬퍼 메서드 사용

        // 2. 1차 입장문 A, B 조회 (A, B 순서로 정렬)
        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        if (arguments.size() < 2) {
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "데이터 오류: 입장문을 찾을 수 없습니다.");
        }

        ArgumentInitial argA = arguments.get(0);
        ArgumentInitial argB = arguments.get(1);

        // [추가] 보안 검증
        if (aCase.getMode() == CaseMode.SOLO && !argA.getUser().getId().equals(user.getId())) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "해당 사건에 대한 조회 권한이 없습니다.");
        }
        // TODO: (추후 확장) 파티 모드일 경우, 참여자인지 확인하는 로직 추가

        rankingService.addCaseScore(caseId,1.0);

        // 3. DTO 빌드
        return CaseDetailResponseDto.builder()
                .aCase(aCase)
                .argA(argA)
                .argB(argB)
                .build();
    }

    // --- [ 1차 재판 상태 변경 (종료) ] ---
    // (이 메서드는 2차 재판 시작 로직과 분리되어야 함)
    @Transactional
    public void updateCaseStatus(Long caseId, CaseStatusRequestDto requestDto, User user) {
        Case foundCase = findCaseById(caseId);
        // TODO: 본인 확인 로직

        // 2차 재판 시작은 별도의 /appeal API를 이용하도록 명시
        if (requestDto.getStatus() == CaseStatus.SECOND) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "2차 재판 시작은 /appeal API를 이용해주세요.");
        }

        // DONE 상태만 허용 (2차 재판은 startAppeal 메서드로)
        if (requestDto.getStatus() == CaseStatus.DONE) {
            foundCase.updateStatus(CaseStatus.DONE);
        } else {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 상태 값입니다. DONE만 가능합니다.");
        }
    }


    // --- [ 2차 재판 신규 메서드 ] ---

    /**
     * 2차 재판 시작 (시간제한 없음)
     */
    @Transactional
    public void startAppeal(Long caseId, User user) {
        Case foundCase = findCaseById(caseId);
        // TODO: 본인 확인 로직

        // 이미 2차 재판이 시작되었거나 종료된 사건에 대한 요청 방지
        if (foundCase.getStatus() != CaseStatus.FIRST) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "이미 2차 재판이 시작되었거나 종료된 사건입니다.");
        }

        foundCase.startAppeal(); // 상태를 SECOND로 변경

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
    public CaseDetail2ndResponseDto getCaseDefenses(Long caseId, User user) {
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

        // 1. 이 사건의 모든 변론 조회
        List<Defense> defenses = defenseRepository.findAllByaCase_Id(caseId);

        // 2. 내가 좋아요 누른 변론 ID 목록 조회
        Set<Long> userLikedDefenseIds = likeRepository.findAllByUserAndContentType(user, ContentType.DEFENSE)
                .stream().map(Like::getContentId).collect(Collectors.toSet());

        // 3. 이 사건의 모든 반론을 가져와서, 변론 ID별로 개수(count)를 계산 (N+1 방지)
        Map<Long, Long> rebuttalCounts = rebuttalRepository.findAllByDefense_aCase_Id(caseId)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getDefense().getId(), Collectors.counting()));

        // 4. DTO로 변환
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
    // --- [ 3. (신규) 반론 목록 조회 API 로직 ] ---
    @Transactional(readOnly = true)
    public List<RebuttalResponseDto> getRebuttalsByDefense(Long defenseId, User user) {
        // 1. 이 변론의 모든 반론 조회
        List<Rebuttal> rebuttals = rebuttalRepository.findAllByDefense_Id(defenseId);

        // 2. 내가 좋아요 누른 반론 ID 목록 조회
        Set<Long> userLikedRebuttalIds = likeRepository.findAllByUserAndContentType(user, ContentType.REBUTTAL)
                .stream().map(Like::getContentId).collect(Collectors.toSet());

        // 3. DTO의 static 헬퍼 메서드를 사용해 중첩 구조(Tree)로 빌드
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
                .build();

        Defense savedDefense = defenseRepository.save(defense);

        rankingService.addCaseScore(caseId,5.0);

        // AI 판결 업데이트 이벤트 발행 (비동기로 AI 판결을 요청)
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

        checkCaseStatusIsSecond(defense.getACase()); // 2차 재판 상태인지 확인

        Rebuttal parentRebuttal = null;
        if (requestDto.getParentId() != null) {
            parentRebuttal = rebuttalRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "부모 반론을 찾을 수 없습니다."));

            // 부모 반론이 요청된 변론에 속하는지 확인
            if (!parentRebuttal.getDefense().getId().equals(defense.getId())) {
                throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 부모 반론 ID 입니다. 해당 변론에 속하지 않습니다.");
            }
        }

        Rebuttal rebuttal = Rebuttal.builder()
                .defense(defense)
                .user(user)
                .parent(parentRebuttal) // 부모 반론 설정 (대댓글)
                .type(requestDto.getType()) // A/B 입장
                .content(requestDto.getContent())
                .build();

        Rebuttal savedRebuttal = rebuttalRepository.save(rebuttal);

        rankingService.addCaseScore(defense.getACase().getId(),5.0);

        // AI 판결 업데이트 이벤트 발행
        eventPublisher.publishEvent(new UpdateJudgmentEvent(defense.getACase().getId()));

        return savedRebuttal;
    }

    /**
     * 배심원 투표
     */
    @Transactional
    public void castVote(Long caseId, VoteRequestDto requestDto, User user) {
        Case aCase = findCaseById(caseId);
        checkCaseStatusIsSecond(aCase); // 2차 재판 상태인지 확인

        Vote vote = voteRepository.findByaCase_IdAndUser_Id(caseId, user.getId())
                .map(existingVote -> {
                    // 이미 투표했다면, 입장 변경
                    existingVote.updateChoice(requestDto.getChoice());
                    return existingVote;
                })
                .orElseGet(() -> {
                    // 첫 투표라면, 새로 생성
                    return Vote.builder()
                            .aCase(aCase)
                            .user(user)
                            .type(requestDto.getChoice())
                            .build();
                });

        voteRepository.save(vote);

        rankingService.addCaseScore(caseId,3.0);

        // AI 판결 업데이트 이벤트 발행
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
     * @Async 어노테이션을 통해 별도의 스레드에서 실행됩니다.
     */
    @Async
    @Transactional
    public void updateFinalJudgment(Long caseId) {
        log.info("Starting async judgment update for caseId: {}", caseId);
        Case aCase = findCaseById(caseId);

        // 1. 추천수 상위 5개 변론 조회
        List<Defense> topDefenses = defenseRepository.findTop5ByaCase_IdOrderByLikesCountDesc(caseId);

        // 2. 투표 결과 조회
        long votesA = voteRepository.countByaCase_IdAndType(caseId, DebateSide.A);
        long votesB = voteRepository.countByaCase_IdAndType(caseId, DebateSide.B);

        // 3. AI에게 판결 요청 (새로운 2차 판결 메서드 호출)
        AiJudgmentDto aiResult = chatGptService2.requestFinalJudgment(aCase, topDefenses, votesA, votesB);

        // 4. 'FINAL' 판결문 조회 및 업데이트 (없으면 예외 발생 - startAppeal에서 생성 보장)
        Judgment finalJudgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.FINAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "2차 판결문이 생성되지 않았습니다."));

        // AI 결과로 판결문 내용 업데이트
        finalJudgment.updateJudgment(
                aiResult.getVerdict(),
                aiResult.getConclusion(),
                aiResult.getRatioA(),
                aiResult.getRatioB()
        );
        judgmentRepository.save(finalJudgment); // 변경 내용 저장
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
}