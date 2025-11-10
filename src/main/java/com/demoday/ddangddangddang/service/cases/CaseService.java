package com.demoday.ddangddangddang.service.cases;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import com.demoday.ddangddangddang.service.ChatGptService2;
import com.demoday.ddangddangddang.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final JudgmentRepository judgmentRepository;
    private final ChatGptService2 chatGptService2;
    private final RankingService rankingService;
    private final CaseParticipationRepository caseParticipationRepository;

    @Transactional
    public CaseResponseDto createCase(CaseRequestDto requestDto, User user) {
        if (!requestDto.getMode().equals(CaseMode.SOLO)) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "현재 솔로 모드만 지원합니다.");
        }

        Case newCase = Case.builder()
                .mode(requestDto.getMode())
                .title(requestDto.getTitle())
                .status(CaseStatus.FIRST)
                .build();
        caseRepository.save(newCase);

        CaseParticipation caseParticipation = CaseParticipation.builder()
                .user(user)
                .aCase(newCase)
                .result(CaseResult.PENDING)
                .build();
        caseParticipationRepository.save(caseParticipation);

        ArgumentInitial argumentA = ArgumentInitial.builder()
                .aCase(newCase)
                .user(user)
                .type(DebateSide.A)
                .mainArgument(requestDto.getArgumentAMain())
                .reasoning(requestDto.getArgumentAReasoning())
                .build();
        argumentInitialRepository.save(argumentA);

        ArgumentInitial argumentB = ArgumentInitial.builder()
                .aCase(newCase)
                .user(user)
                .type(DebateSide.B)
                .mainArgument(requestDto.getArgumentBMain())
                .reasoning(requestDto.getArgumentBReasoning())
                .build();
        argumentInitialRepository.save(argumentB);

        List<ArgumentInitial> arguments = List.of(argumentA, argumentB);
        AiJudgmentDto aiResult = chatGptService2.getAiJudgment(newCase, arguments);

        Judgment judgment = Judgment.builder()
                .aCase(newCase)
                .stage(JudgmentStage.INITIAL)
                .content(aiResult.getVerdict())
                .basedOn(aiResult.getConclusion())
                .ratioA(aiResult.getRatioA())
                .ratioB(aiResult.getRatioB())
                .build();
        judgmentRepository.save(judgment);

        return new CaseResponseDto(newCase.getId());
    }

    @Transactional(readOnly = true)
    public JudgmentResponseDto getJudgment(Long caseId, User user) {
        Case foundCase = findCaseById(caseId);
        // TODO: 본인 확인 로직

        Judgment judgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.INITIAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "아직 판결이 완료되지 않았습니다."));

        return new JudgmentResponseDto(judgment);
    }

    @Transactional(readOnly = true)
    public CaseDetailResponseDto getCaseDetail(Long caseId, User user) {
        Case aCase = findCaseById(caseId);

        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        if (arguments.size() < 2) {
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "데이터 오류: 입장문을 찾을 수 없습니다.");
        }

        ArgumentInitial argA = arguments.get(0);
        ArgumentInitial argB = arguments.get(1);

        if (aCase.getMode() == CaseMode.SOLO && !argA.getUser().getId().equals(user.getId())) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "해당 사건에 대한 조회 권한이 없습니다.");
        }

        rankingService.addCaseScore(caseId, 1.0); // ⭐️ 조회수 증가 로직

        return CaseDetailResponseDto.builder()
                .aCase(aCase)
                .argA(argA)
                .argB(argB)
                .build();
    }

    @Transactional
    public void updateCaseStatus(Long caseId, CaseStatusRequestDto requestDto, User user) {
        Case foundCase = findCaseById(caseId);
        // TODO: 본인 확인 로직

        if (requestDto.getStatus() == CaseStatus.SECOND) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "2차 재판 시작은 /appeal API를 이용해주세요.");
        }

        if (requestDto.getStatus() == CaseStatus.DONE) {
            foundCase.updateStatus(CaseStatus.DONE);
        } else {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 상태 값입니다. DONE만 가능합니다.");
        }
    }

    // --- 공통 헬퍼 메서드 ---
    private Case findCaseById(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "사건을 찾을 수 없습니다."));
    }
}