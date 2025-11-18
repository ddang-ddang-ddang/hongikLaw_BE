package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    //판결문 저장
    public ApiResponse<Long> createJudge(Long caseId, FinalJudgmentRequestDto voteDto, Long userId) {

        // 1. Case 엔티티 조회
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        User foundUser = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        //유저가 initial한 사건인지 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(foundCase);

        //하나라도 유저가 참여한 항목 반환
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));


        // 2. [기존 로직] 채택된 변론/반론 조회 (AI 호출 및 basedOn JSON에 모두 사용)
        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdopted(caseId, true);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        // 3. ChatGPT 서비스 호출 (수정된 메서드 사용)
        AiJudgmentDto aiResult = chatGptService2.requestFinalJudgment(
                foundCase,
                adoptedDefenses,  // <-- 조회한 '채택된' 변론 전달
                adoptedRebuttals, // <-- 조회한 '채택된' 반론 전달
                voteDto.getVotesA(),
                voteDto.getVotesB()
        );

        // 4. [기존 로직] 'basedOn'에 사용할 ID 목록 JSON 생성
        List<Long> adoptedDefenseIds = adoptedDefenses.stream()
                .map(Defense::getId)
                .toList();
        List<Long> adoptedRebuttalIds = adoptedRebuttals.stream()
                .map(Rebuttal::getId)
                .toList();

        JudgementBasisDto basisDto = new JudgementBasisDto(adoptedDefenseIds, adoptedRebuttalIds);
        String basedOnJson;
        try {
            basedOnJson = objectMapper.writeValueAsString(basisDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize judgment basis", e);
        }

        // 5. Judgment 엔티티 생성 (AI 결과 사용)
        Judgment finalJudgment = Judgment.builder()
                .aCase(foundCase)
                .stage(JudgmentStage.FINAL)
                .content(aiResult.getVerdict()+aiResult.getConclusion())   // AI가 생성한 판결문
                .ratioA(aiResult.getRatioA())       // AI가 생성한 비율
                .ratioB(aiResult.getRatioB())
                .basedOn(basedOnJson) // "근거"로 JSON 문자열 저장
                .build();

        judgmentRepository.save(finalJudgment);

        DebateSide winSide = DebateSide.DRAW;
        if(finalJudgment.getRatioA()>finalJudgment.getRatioB()) {winSide = DebateSide.A;}
        else if (finalJudgment.getRatioA()<finalJudgment.getRatioB()){
            winSide = DebateSide.B;
        }

        //사건-유저 승패 결과 기록
        List<CaseParticipation> caseParticipations = caseParticipationRepository.findByaCase(foundCase);
        for (CaseParticipation caseParticipation : caseParticipations) {
            User user = caseParticipation.getUser();
            List<ArgumentInitial> argumentInitials = argumentInitialRepository.findByaCaseAndUser(foundCase,user);
            if(argumentInitials.size() > 1) {caseParticipation.updateResult(CaseResult.SOLO); break;}
            for(ArgumentInitial argumentInitial : argumentInitials) {
                if(argumentInitial.getType()== winSide) {caseParticipation.updateResult(CaseResult.WIN); caseParticipation.getUser().updateExp(150L);}
                else if (winSide == DebateSide.DRAW) {
                    caseParticipation.updateResult(CaseResult.DRAW);
                }
                else caseParticipation.updateResult(CaseResult.LOSE);
            }
        }

        //변론 유저 승패 결과 기록
        for (Defense adoptDefense : adoptedDefenses ) {
            if(adoptDefense.getType() == winSide){
                adoptDefense.updateResult(CaseResult.WIN);
                adoptDefense.getUser().updateExp(150L);
            }
            else if (winSide == DebateSide.DRAW){
                adoptDefense.updateResult(CaseResult.DRAW);
            }
            else adoptDefense.updateResult(CaseResult.LOSE);
        }

        for(Rebuttal adoptedRebuttal : adoptedRebuttals){
            if(adoptedRebuttal.getType() == winSide) {adoptedRebuttal.updateResult(CaseResult.WIN);
            adoptedRebuttal.getUser().updateExp(150L);}
            else if (winSide == DebateSide.DRAW){adoptedRebuttal.updateResult(CaseResult.DRAW);}
            else adoptedRebuttal.updateResult(CaseResult.LOSE);
        }

        //투표 유저 경험치 증가
        if(winSide != DebateSide.DRAW){
            List<Vote> winVotes = voteRepository.findByaCase_IdAndType(foundCase.getId(), winSide);
            for (Vote winVote : winVotes) {
                winVote.getUser().updateExp(20L);
            }
        }

        return ApiResponse.onSuccess("성공적으로 판결이 저장되었습니다.", finalJudgment.getId());
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
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        // 1. [변경] 최종심(THIRD) 상태가 아니면 중지
        if (aCase.getStatus() != CaseStatus.THIRD) {
            log.warn("Case {} is not in THIRD status. Skipping snapshot.", caseId);
            return;
        }

        // --- 2. A/B 진영별 '좋아요 Top 5' 항목 조회 ---
//        List<Defense> topDefensesA = defenseRepository.findTop5ByaCase_IdAndTypeOrderByLikesCountDesc(caseId, DebateSide.A);
//        List<Rebuttal> topRebuttalsA = rebuttalRepository.findTop5ByDefense_aCase_IdAndTypeOrderByLikesCountDesc(caseId, DebateSide.A);
//        List<Defense> topDefensesB = defenseRepository.findTop5ByaCase_IdAndTypeOrderByLikesCountDesc(caseId, DebateSide.B);
//        List<Rebuttal> topRebuttalsB = rebuttalRepository.findTop5ByDefense_aCase_IdAndTypeOrderByLikesCountDesc(caseId, DebateSide.B);
//
//        List<Defense> allTopDefenses = Stream.concat(topDefensesA.stream(), topDefensesB.stream()).toList();
//        List<Rebuttal> allTopRebuttals = Stream.concat(topRebuttalsA.stream(), topRebuttalsB.stream()).toList();

        List<Defense> allDefensesA = defenseRepository.findAllByaCase_IdAndType(caseId,DebateSide.A);
        List<Rebuttal> allRebuttalsA = rebuttalRepository.findAllByDefense_aCase_IdAndType(caseId,DebateSide.A);
        List<Defense> allDefensesB = defenseRepository.findAllByaCase_IdAndType(caseId,DebateSide.B);
        List<Rebuttal> allRebuttalsB = rebuttalRepository.findAllByDefense_aCase_IdAndType(caseId,DebateSide.B);

        Stream<AdoptableItemDto> defenseStreamA = allDefensesA.stream()
                .map(d ->{
                    d.markAsAdoptedFalse();
                    return AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .caseId(d.getACase().getId())
                        .userId(d.getUser().getId())
                        .debateSide(d.getType())
                        .content(d.getContent())
                        .likeCount(d.getLikesCount())
                        // defenseId, parentId, parentContent는 null (자동)
                        .build();}
                );

        Stream<AdoptableItemDto> defenseStreamB = allDefensesB.stream()
                .map(d -> {
                    d.markAsAdoptedFalse();
                    return AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .caseId(d.getACase().getId())
                        .userId(d.getUser().getId())
                        .debateSide(d.getType())
                        .content(d.getContent())
                        .likeCount(d.getLikesCount())
                        // defenseId, parentId, parentContent는 null (자동)
                        .build();}
                );

        Stream<AdoptableItemDto> rebuttalStreamA = allRebuttalsA.stream()
                .map(r -> {
                    Rebuttal parent = r.getParent(); r.markAsAdoptedFalse();
                    return AdoptableItemDto.builder()
                            .itemType(ContentType.REBUTTAL)
                            .id(r.getId())
                            .caseId(r.getDefense().getACase().getId())
                            .userId(r.getUser().getId())
                            .debateSide(r.getType())
                            .content(r.getContent())
                            .likeCount(r.getLikesCount())
                            // --- Rebuttal 전용 필드 ---
                            .defenseId(r.getDefense().getId())
                            .parentId((parent != null) ? parent.getId() : null)
                            .parentContent((parent != null) ? parent.getContent() : null)
                            .build();
                });

        Stream<AdoptableItemDto> rebuttalStreamB = allRebuttalsB.stream()
                .map(r -> {
                    Rebuttal parent = r.getParent(); r.markAsAdoptedFalse();
                    return AdoptableItemDto.builder()
                            .itemType(ContentType.REBUTTAL)
                            .id(r.getId())
                            .caseId(r.getDefense().getACase().getId())
                            .userId(r.getUser().getId())
                            .debateSide(r.getType())
                            .content(r.getContent())
                            .likeCount(r.getLikesCount())
                            // --- Rebuttal 전용 필드 ---
                            .defenseId(r.getDefense().getId())
                            .parentId((parent != null) ? parent.getId() : null)
                            .parentContent((parent != null) ? parent.getContent() : null)
                            .build();
                });

        // 3. [변경] 두 스트림을 합치고(concat), 정렬(sorted)하고, 5개만(limit) 선택
        List<AdoptableItemDto> top5ItemsA = Stream.concat(defenseStreamA, rebuttalStreamA)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed()) // 좋아요 순 정렬
                .limit(5)
                .toList();

        List<AdoptableItemDto> top5ItemsB = Stream.concat(defenseStreamB, rebuttalStreamB)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed()) // 좋아요 순 정렬
                .limit(5)
                .toList();

        List<Long> topItemIdsA = top5ItemsA.stream().map(AdoptableItemDto::getId).toList();
        List<Long> topItemIdsB = top5ItemsB.stream().map(AdoptableItemDto::getId).toList();

        JudgementBasisDto currentBasisDto = new JudgementBasisDto(topItemIdsA, topItemIdsB);

        for (AdoptableItemDto item : top5ItemsA) {
            if (item.getItemType() == ContentType.DEFENSE) {
                Defense defense = defenseRepository.findById(item.getId())
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "자동 채택할 변론을 찾을 수 없습니다."));
                defense.markAsAdopted();
                //defense.getUser().updateExp(100L); // 의견 작성자에게 경험치 부여

            } else if (item.getItemType() == ContentType.REBUTTAL) {
                Rebuttal rebuttal = rebuttalRepository.findById(item.getId())
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "자동 채 채택할 반론을 찾을 수 없습니다."));
                rebuttal.markAsAdopted();
                //rebuttal.getUser().updateExp(100L); // 의견 작성자에게 경험치 부여
            }
        }

        for (AdoptableItemDto item : top5ItemsB) {
            if (item.getItemType() == ContentType.DEFENSE) {
                Defense defense = defenseRepository.findById(item.getId())
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "자동 채택할 변론을 찾을 수 없습니다."));
                defense.markAsAdopted();
                //defense.getUser().updateExp(100L); // 의견 작성자에게 경험치 부여

            } else if (item.getItemType() == ContentType.REBUTTAL) {
                Rebuttal rebuttal = rebuttalRepository.findById(item.getId())
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "자동 채 채택할 반론을 찾을 수 없습니다."));
                rebuttal.markAsAdopted();
                //rebuttal.getUser().updateExp(100L); // 의견 작성자에게 경험치 부여
            }
        }

        String currentBasisJson;
        try {
            currentBasisJson = objectMapper.writeValueAsString(currentBasisDto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize current basis for caseId: {}", caseId, e);
            throw new RuntimeException("Failed to serialize current basis", e);
        }

        // --- 3. 가장 최근 판결의 '기준'과 '현재 기준' 비교 ---
        Optional<Judgment> latestJudgmentOpt = judgmentRepository.findTopByaCase_IdAndStageOrderByCreatedAtDesc(caseId, JudgmentStage.FINAL);

        if (latestJudgmentOpt.isPresent()) {
            String lastBasisJson = latestJudgmentOpt.get().getBasedOn();
            if (currentBasisJson.equals(lastBasisJson)) {
                log.info("Top 5 items (per side) unchanged for case {}. Skipping snapshot.", caseId);
                return; // 변경 내역 없음
            }
        }

        // --- 4. AI 판결 요청 및 새 스냅샷 저장 ---
        log.info("Top 5 items (per side) changed for case {}. Creating new snapshot.", caseId);

        long votesA = voteRepository.countByaCase_IdAndType(caseId, DebateSide.A);
        long votesB = voteRepository.countByaCase_IdAndType(caseId, DebateSide.B);

        List<Defense> topDefense = defenseRepository.findByaCase_IdAndIsAdopted(caseId,true);
        List<Rebuttal> topRebuttal = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        AiJudgmentDto aiResult = chatGptService2.requestFinalJudgment(
                aCase, topDefense, topRebuttal, votesA, votesB
        );

        Judgment snapshotJudgment = Judgment.builder()
                .aCase(aCase)
                .stage(JudgmentStage.FINAL)
                .content(aiResult.getVerdict() + aiResult.getConclusion())
                .basedOn(currentBasisJson)
                .ratioA(aiResult.getRatioA())
                .ratioB(aiResult.getRatioB())
                .build();

        judgmentRepository.save(snapshotJudgment);
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
}
