package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
import com.demoday.ddangddangddang.domain.event.WinEvent;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.third.*;
import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import com.demoday.ddangddangddang.service.ChatGptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FinalJudgeService {
    private final JudgmentRepository judgmentRepository;
    private final DefenseRepository defenseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final CaseRepository caseRepository; // aCase를 가져오기 위함
    private final ObjectMapper objectMapper;
    private final ChatGptService chatGptService2;
    private final CaseParticipationRepository caseParticipationRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 1. [ReadOnly Transaction] 판결에 필요한 데이터를 미리 조회
     */
    @Transactional(readOnly = true)
    public JudgeContextDto prepareJudgeContext(Long caseId, Long userId) {
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        // 유저가 initial한 사건인지 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(foundCase);

        boolean isParticipant = allInitialArguments.stream()
                .anyMatch(arg -> arg.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE);
        }

        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdoptedAndIsBlindFalse(caseId, true);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        return new JudgeContextDto(foundCase, adoptedDefenses, adoptedRebuttals);
    }

    /**
     * 2. [Write Transaction] AI 결과를 DB에 저장하고 승패 정산
     */
    @Transactional
    public Long saveJudgeResultAndSettle(Long caseId, AiJudgmentDto aiResult) {
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdoptedAndIsBlindFalse(caseId, true);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        List<Long> adoptedDefenseIds = adoptedDefenses.stream().map(Defense::getId).toList();
        List<Long> adoptedRebuttalIds = adoptedRebuttals.stream().map(Rebuttal::getId).toList();

        JudgementBasisDto basisDto = new JudgementBasisDto(adoptedDefenseIds, adoptedRebuttalIds);
        String basedOnJson;
        try {
            basedOnJson = objectMapper.writeValueAsString(basisDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize judgment basis", e);
        }

        Judgment finalJudgment = Judgment.builder()
                .aCase(foundCase)
                .stage(JudgmentStage.FINAL)
                .content(aiResult.getVerdict() + aiResult.getConclusion())
                .ratioA(aiResult.getRatioA())
                .ratioB(aiResult.getRatioB())
                .basedOn(basedOnJson)
                .build();

        judgmentRepository.save(finalJudgment);

        DebateSide winSide = DebateSide.DRAW;
        if (finalJudgment.getRatioA() > finalJudgment.getRatioB()) {
            winSide = DebateSide.A;
        } else if (finalJudgment.getRatioA() < finalJudgment.getRatioB()) {
            winSide = DebateSide.B;
        }

        // 정산 로직 (N+1 최적화 없이 유지)
        settleResults(foundCase, winSide, adoptedDefenses, adoptedRebuttals);

        return finalJudgment.getId();
    }

    private void settleResults(Case foundCase, DebateSide winSide, List<Defense> adoptedDefenses, List<Rebuttal> adoptedRebuttals) {
        // 사건-유저 승패 결과 기록
        List<CaseParticipation> caseParticipations = caseParticipationRepository.findByaCase(foundCase);
        for (CaseParticipation caseParticipation : caseParticipations) {
            User user = caseParticipation.getUser();
            List<ArgumentInitial> argumentInitials = argumentInitialRepository.findByaCaseAndUser(foundCase, user);

            //유저가 한 사건에 대해 발행한 초기 의견이 1개 이상이면 솔로모드이므로 break;
            if (argumentInitials.size() > 1) {
                caseParticipation.updateResult(CaseResult.SOLO);
                break;
            }
            for (ArgumentInitial argumentInitial : argumentInitials) {
                if (argumentInitial.getType() == winSide) {
                    caseParticipation.updateResult(CaseResult.WIN);
                    eventPublisher.publishEvent(new WinEvent(user));
                    caseParticipation.getUser().updateExp(150L);
                    caseParticipation.getUser().updateWin();
                } else if (winSide == DebateSide.DRAW) {
                    caseParticipation.updateResult(CaseResult.DRAW);
                } else {
                    caseParticipation.updateResult(CaseResult.LOSE);
                    caseParticipation.getUser().updateLose();
                }
            }
        }

        // 변론 유저 승패 결과 기록
        for (Defense adoptDefense : adoptedDefenses) {
            if (adoptDefense.getType() == winSide) {
                adoptDefense.updateResult(CaseResult.WIN);
                adoptDefense.getUser().updateExp(150L);
                adoptDefense.getUser().updateWin();
                eventPublisher.publishEvent(new WinEvent(adoptDefense.getUser()));
            } else if (winSide == DebateSide.DRAW) {
                adoptDefense.updateResult(CaseResult.DRAW);
            } else {
                adoptDefense.updateResult(CaseResult.LOSE);
                adoptDefense.getUser().updateLose();
            }
        }

        for (Rebuttal adoptedRebuttal : adoptedRebuttals) {
            if (adoptedRebuttal.getType() == winSide) {
                adoptedRebuttal.updateResult(CaseResult.WIN);
                adoptedRebuttal.getUser().updateExp(150L);
                adoptedRebuttal.getUser().updateWin();
                eventPublisher.publishEvent(new WinEvent(adoptedRebuttal.getUser()));
            } else if (winSide == DebateSide.DRAW) {
                adoptedRebuttal.updateResult(CaseResult.DRAW);
            } else {
                adoptedRebuttal.updateResult(CaseResult.LOSE);
                adoptedRebuttal.getUser().updateLose();
            }
        }

        // 투표 유저 경험치 증가
        if (winSide != DebateSide.DRAW) {
            List<Vote> winVotes = voteRepository.findByaCase_IdAndType(foundCase.getId(), winSide);
            for (Vote winVote : winVotes) {
                winVote.getUser().updateExp(20L);
            }
        }
    }

    //판결문 조회
    public ApiResponse<JudgementDetailResponseDto> getFinalJudgemnet(Long caseId){
        // 1. caseId와 'FINAL' 스테이지로 판결(Judgment) 엔티티 조회
        Judgment judgment = judgmentRepository.findTopByaCase_IdAndStageOrderByCreatedAtDesc(caseId, JudgmentStage.FINAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.JUDGE_NOT_FOUND, "판결을 찾을 수 없습니다."));

        // 2. basedOn 필드의 JSON 문자열을 DTO로 파싱 (역직렬화)
        String basedOnJson = judgment.getBasedOn();
        JudgementBasisDto basisDto;
        try {
            if (basedOnJson == null || basedOnJson.isEmpty()) {
                // 근거가 없는 경우 (JSON이 비어있는 경우)
                basisDto = new JudgementBasisDto(Collections.emptyList(), Collections.emptyList());
            } else {
                basisDto = objectMapper.readValue(basedOnJson, JudgementBasisDto.class);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse judgment basis", e);
        }

        // 3. 파싱한 ID 리스트로 실제 Defense, Rebuttal 엔티티 목록 조회
        List<Defense> adoptedDefenses = defenseRepository.findAllById(basisDto.getDefenseIds());
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAllById(basisDto.getRebuttalIds());

        // 4. 엔티티 리스트 -> DTO 리스트로 변환 (이전 'getAdopt' 메서드 로직 재활용)
        List<AdoptResponseDto.DefenseAdoptDto> defenseDtos = adoptedDefenses.stream()
                .map(defense -> AdoptResponseDto.DefenseAdoptDto.builder()
                        .caseId(defense.getACase().getId())
                        .userId(defense.getUser().getId())
                        .defenseId(defense.getId())
                        .debateSide(defense.getType())
                        .content(defense.getContent())
                        .likeCount(defense.getLikesCount())
                        .build())
                .toList();

        List<AdoptResponseDto.RebuttalAdoptDto> rebuttalDtos = adoptedRebuttals.stream()
                .map(rebuttal -> AdoptResponseDto.RebuttalAdoptDto.builder()
                        .caseId(rebuttal.getDefense().getACase().getId())
                        .userId(rebuttal.getUser().getId())
                        .defenseId(rebuttal.getDefense().getId())
                        .rebuttalId(rebuttal.getId())
                        .debateSide(rebuttal.getType())
                        .content(rebuttal.getContent())
                        .likeCount(rebuttal.getLikesCount())
                        .build())
                .toList();

        // 5. 최종 응답 DTO (JudgmentDetailResponseDto) 빌드
        JudgementDetailResponseDto responseDto = JudgementDetailResponseDto.builder()
                .judgmentId(judgment.getId())
                .content(judgment.getContent())
                .ratioA(judgment.getRatioA())
                .ratioB(judgment.getRatioB())
                .adoptedDefenses(defenseDtos)
                .adoptedRebuttals(rebuttalDtos)
                .build();

        return ApiResponse.onSuccess("판결문 및 채택 근거 조회 완료", responseDto);
    }

    /**
     * [비동기] 스케줄러가 호출하는 '일일 판결 스냅샷 생성'
     * (A/B 진영별 Top 5가 변경되었을 때만 생성)
     */
    @Async
    @Transactional
    public void createDailyJudgmentSnapshot(Long caseId) {
        log.info("Checking daily judgment snapshot for caseId: {}", caseId);

        Case aCase = getThirdStageCaseOrNull(caseId);
        if (aCase == null) {
            return;
        }

        // 1. A/B 진영별 전체 변론/반론 로딩
        SideItems sideAItems = loadSideItems(caseId, DebateSide.A);
        SideItems sideBItems = loadSideItems(caseId, DebateSide.B);

        // 2. 기존 채택 모두 해제
        resetAdoptedFlags(sideAItems, sideBItems);

        // 3. A/B 진영별 좋아요 Top 5 계산
        List<AdoptableItemDto> top5ItemsA = pickTop5(sideAItems);
        List<AdoptableItemDto> top5ItemsB = pickTop5(sideBItems);

        JudgementBasisDto currentBasisDto = buildJudgementBasis(top5ItemsA, top5ItemsB);
        String currentBasisJson = toJson(currentBasisDto);

        // 4. 기준이 이전 스냅샷과 동일하면 스킵
        if (!isBasisChanged(caseId, currentBasisJson)) {
            log.info("Top 5 items (per side) unchanged for case {}. Skipping snapshot.", caseId);
            return;
        }

        // 5. Top5 항목 채택 플래그 설정
        markTopItemsAsAdopted(top5ItemsA, sideAItems);
        markTopItemsAsAdopted(top5ItemsB, sideBItems);

        // 6. AI 판결 요청 & 스냅샷 저장
        AiJudgmentDto aiResult = requestFinalAiJudgment(aCase);
        saveSnapshot(aCase, currentBasisJson, aiResult);

        log.info("Finished daily judgment snapshot for caseId: {}", caseId);
    }


    /**
     * 판결 히스토리 조회 (아카이브)
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<JudgmentResponseDto>> getJudgmentHistory(Long caseId) {
        // 2. FINAL 스테이지의 모든 판결을 '오래된 순'으로 조회
        List<Judgment> history = judgmentRepository.findAllByaCase_IdAndStageOrderByCreatedAtAsc(caseId, JudgmentStage.FINAL);

        if (history.isEmpty()) {
            return ApiResponse.onSuccess("아직 판결 히스토리가 없습니다.", Collections.emptyList());
        }

        // 3. JudgmentResponseDto 리스트로 변환
        List<JudgmentResponseDto> historyDtos = history.stream()
                .map(judgment -> new JudgmentResponseDto(judgment)) // DTO 재활용
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("판결 히스토리 조회 완료", historyDtos);
    }

    private Case getThirdStageCaseOrNull(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        if (aCase.getStatus() != CaseStatus.THIRD) {
            log.warn("Case {} is not in THIRD status. Skipping snapshot.", caseId);
            return null;
        }
        return aCase;
    }

    /**
     * 한 진영(A/B)에 대한 변론/반론 목록을 묶어서 들고 다니기 위한 DTO
     */
    @RequiredArgsConstructor
    @Getter
    private static class SideItems {
        private final DebateSide side;
        private final List<Defense> defenses;
        private final List<Rebuttal> rebuttals;
    }

    private SideItems loadSideItems(Long caseId, DebateSide side) {
        // [FIX for error 321 - Defense] BLIND 미포함 메서드 사용
        List<Defense> defenses = defenseRepository.findAllByaCase_IdAndTypeAndIsBlindFalse(caseId, side);

        // [FIX for error 321 - Rebuttal] BLIND 미포함 메서드 사용
        List<Rebuttal> rebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsBlindFalse(caseId, side);
        return new SideItems(side, defenses, rebuttals);
    }

    private void resetAdoptedFlags(SideItems... sides) {
        for (SideItems sideItems : sides) {
            sideItems.getDefenses().forEach(Defense::markAsAdoptedFalse);
            sideItems.getRebuttals().forEach(Rebuttal::markAsAdoptedFalse);
        }
    }

    private List<AdoptableItemDto> pickTop5(SideItems sideItems) {
        Stream<AdoptableItemDto> defenseStream = sideItems.getDefenses().stream()
                .map(this::toDefenseDto);

        Stream<AdoptableItemDto> rebuttalStream = sideItems.getRebuttals().stream()
                .map(this::toRebuttalDto);

        return Stream.concat(defenseStream, rebuttalStream)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed())
                .limit(5)
                .toList();
    }

    private AdoptableItemDto toDefenseDto(Defense d) {
        return AdoptableItemDto.builder()
                .itemType(ContentType.DEFENSE)
                .id(d.getId())
                .caseId(d.getACase().getId())
                .userId(d.getUser().getId())
                .debateSide(d.getType())
                .content(d.getContent())
                .likeCount(d.getLikesCount())
                .build();
    }

    private AdoptableItemDto toRebuttalDto(Rebuttal r) {
        Rebuttal parent = r.getParent();
        return AdoptableItemDto.builder()
                .itemType(ContentType.REBUTTAL)
                .id(r.getId())
                .caseId(r.getDefense().getACase().getId())
                .userId(r.getUser().getId())
                .debateSide(r.getType())
                .content(r.getContent())
                .likeCount(r.getLikesCount())
                .defenseId(r.getDefense().getId())
                .parentId(parent != null ? parent.getId() : null)
                .parentContent(parent != null ? parent.getContent() : null)
                .build();
    }

    private JudgementBasisDto buildJudgementBasis(List<AdoptableItemDto> top5A,
                                                  List<AdoptableItemDto> top5B) {
        List<Long> idsA = top5A.stream().map(AdoptableItemDto::getId).toList();
        List<Long> idsB = top5B.stream().map(AdoptableItemDto::getId).toList();
        return new JudgementBasisDto(idsA, idsB);
    }

    private String toJson(JudgementBasisDto basisDto) {
        try {
            return objectMapper.writeValueAsString(basisDto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize current basis", e);
            throw new RuntimeException("Failed to serialize current basis", e);
        }
    }

    private boolean isBasisChanged(Long caseId, String currentBasisJson) {
        Optional<Judgment> latestJudgmentOpt =
                judgmentRepository.findTopByaCase_IdAndStageOrderByCreatedAtDesc(
                        caseId, JudgmentStage.FINAL);

        if (latestJudgmentOpt.isEmpty()) {
            return true; // 첫 스냅샷이면 무조건 생성
        }

        String lastBasisJson = latestJudgmentOpt.get().getBasedOn();
        return !currentBasisJson.equals(lastBasisJson);
    }

    private void markTopItemsAsAdopted(List<AdoptableItemDto> topItems, SideItems sideItems) {
        Map<Long, Defense> defenseMap = sideItems.getDefenses().stream()
                .collect(Collectors.toMap(Defense::getId, d -> d));
        Map<Long, Rebuttal> rebuttalMap = sideItems.getRebuttals().stream()
                .collect(Collectors.toMap(Rebuttal::getId, r -> r));

        for (AdoptableItemDto item : topItems) {
            if (item.getItemType() == ContentType.DEFENSE) {
                Defense defense = defenseMap.get(item.getId());
                if (defense == null) {
                    throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER,
                            "자동 채택할 변론을 찾을 수 없습니다.");
                }
                defense.markAsAdopted();
                // defense.getUser().updateExp(100L);

            } else if (item.getItemType() == ContentType.REBUTTAL) {
                Rebuttal rebuttal = rebuttalMap.get(item.getId());
                if (rebuttal == null) {
                    throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER,
                            "자동 채택할 반론을 찾을 수 없습니다.");
                }
                rebuttal.markAsAdopted();
                // rebuttal.getUser().updateExp(100L);
            }
        }
    }

    private AiJudgmentDto requestFinalAiJudgment(Case aCase) {
        Long caseId = aCase.getId();
        long votesA = voteRepository.countByaCase_IdAndType(caseId, DebateSide.A);
        long votesB = voteRepository.countByaCase_IdAndType(caseId, DebateSide.B);

        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdoptedAndIsBlindFalse(aCase.getId(), true);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(aCase.getId());

        return chatGptService2.requestFinalJudgment(
                aCase, adoptedDefenses, adoptedRebuttals, votesA, votesB
        );
    }

    private void saveSnapshot(Case aCase, String basisJson, AiJudgmentDto aiResult) {
        Judgment snapshotJudgment = Judgment.builder()
                .aCase(aCase)
                .stage(JudgmentStage.FINAL)
                .content(aiResult.getVerdict() + aiResult.getConclusion())
                .basedOn(basisJson)
                .ratioA(aiResult.getRatioA())
                .ratioB(aiResult.getRatioB())
                .build();

        judgmentRepository.save(snapshotJudgment);
    }

    public ApiResponse<ShowJudgeStatus> getJudgeStatus(Long caseId){
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND,"존재하지 않는 사건입니다."));

        ShowJudgeStatus status;
        int count = judgmentRepository.countByaCase_IdAndStage(caseId,JudgmentStage.FINAL);
        if(count > 1){
            status = ShowJudgeStatus.AFTER;
        }
        else status = ShowJudgeStatus.BEFORE;

        return ApiResponse.onSuccess("현재 최종판결 상태 조회 완료", status);
    }


}
