package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.Judgment;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.CaseMode;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.domain.enums.JudgmentStage;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.ArgumentInitialRepository;
import com.demoday.ddangddangddang.repository.CaseRepository;
import com.demoday.ddangddangddang.repository.JudgmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseService {


    private final CaseRepository caseRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final JudgmentRepository judgmentRepository;
    private final ChatGptService chatGptService; // <-- [중요] AI 서비스 주입

    // 1차 재판(초심) 생성 (솔로 모드)

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

        // --- [ 4. (수정) 실제 AI 판결 로직 ] ---
        // TODO: AI 호출이 오래 걸릴 수 있으므로, @Async 등을 이용한 비동기 처리로 리팩토링 권장
        List<ArgumentInitial> arguments = List.of(argumentA, argumentB);
        AiJudgmentDto aiResult = chatGptService.getAiJudgment(newCase, arguments);

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
        // --- [ 수정 완료 ] ---

        return new CaseResponseDto(newCase.getId());
    }

    //1차 재판(초심) 결과 조회

    @Transactional(readOnly = true)
    public JudgmentResponseDto getJudgment(Long caseId, User user) {
        // (솔로 모드이므로 본인이 생성한 사건인지 확인하는 로직 추가)
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "사건을 찾을 수 없습니다."));

        // TODO: 본인 확인 로직

        // 초심(INITIAL) 판결문 조회
        Judgment judgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.INITIAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "아직 판결이 완료되지 않았습니다."));

        // [수정] DTO 생성자 변경
        return new JudgmentResponseDto(judgment);
    }

    //1차 재판 상태 변경 (종료 또는 2차로)
    @Transactional
    public void updateCaseStatus(Long caseId, CaseStatusRequestDto requestDto, User user) {
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "사건을 찾을 수 없습니다."));

        // TODO: 본인 확인 로직

        // 요청된 상태(DONE 또는 SECOND)로 변경
        foundCase.updateStatus(requestDto.getStatus());
    }

    // --- [ 1. 이 메서드를 새로 추가 ] ---
    @Transactional(readOnly = true)
    public CaseDetailResponseDto getCaseDetail(Long caseId, User user) {
        // 1. 사건 조회
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "해당 사건을 찾을 수 없습니다."));

        // 2. 1차 입장문 A, B 조회 (A, B 순서로 정렬)
        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        if (arguments.size() < 2) {
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "데이터 오류: 입장문을 찾을 수 없습니다.");
        }

        ArgumentInitial argA = arguments.get(0);
        ArgumentInitial argB = arguments.get(1);

        // 3. DTO 빌드
        return CaseDetailResponseDto.builder()
                .aCase(aCase)
                .argA(argA)
                .argB(argB)
                .build();
    }
}