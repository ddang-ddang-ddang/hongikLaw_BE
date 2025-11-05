package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Judgment;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.enums.JudgmentStage;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.dto.third.JudgementBasisDto;
import com.demoday.ddangddangddang.dto.third.JudgementDetailResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FinalJudgeService {
    private final JudgmentRepository judgmentRepository;
    private final DefenseRepository defenseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final CaseRepository caseRepository; // aCase를 가져오기 위함
    private final ObjectMapper objectMapper;

    //판결문 저장
    public ApiResponse<Long> createJudge(Long caseId, String aiContent, Integer ratioA, Integer ratioB){
        List<Defense> defenses = defenseRepository.findByACase_IdAndIsAdopted(caseId, true);
        List<Rebuttal> rebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        List<Long> adoptedDefenseIds = defenses.stream()
                .map(Defense::getId)
                .toList();

        List<Long> adoptedRebuttalIds = rebuttals.stream()
                .map(Rebuttal::getId)
                .toList();

        // 3. DTO 생성 및 JSON 문자열로 변환
        JudgementBasisDto basisDto = new JudgementBasisDto(adoptedDefenseIds, adoptedRebuttalIds);
        String basedOnJson;
        try {
            basedOnJson = objectMapper.writeValueAsString(basisDto);
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시 예외 처리
            throw new RuntimeException("Failed to serialize judgment basis", e);
        }

        // 4. Case 엔티티 조회
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        // 5. Judgment 엔티티 생성 및 저장
        Judgment finalJudgment = Judgment.builder()
                .aCase(foundCase)
                .stage(JudgmentStage.FINAL)
                .content(aiContent)   // AI가 생성한 판결문
                .ratioA(ratioA)       // AI가 생성한 비율
                .ratioB(ratioB)
                .basedOn(basedOnJson) // "근거"로 JSON 문자열 저장
                .build();

        judgmentRepository.save(finalJudgment);
        return ApiResponse.onSuccess("성공적으로 판결이 저장되었습니다.", finalJudgment.getId());
    }

    //판결문 조회
    public ApiResponse<JudgementDetailResponseDto> getFinalJudgemnet(Long caseId){
        // 1. caseId와 'FINAL' 스테이지로 판결(Judgment) 엔티티 조회
        Judgment judgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.FINAL)
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
                .createdAt(judgment.getCreatedAt()) // (Judgment의 createdAt 필드 사용)
                .adoptedDefenses(defenseDtos)
                .adoptedRebuttals(rebuttalDtos)
                .build();

        return ApiResponse.onSuccess("판결문 및 채택 근거 조회 완료", responseDto);
    }
}
