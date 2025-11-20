package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.second.CaseDetail2ndResponseDto;
import com.demoday.ddangddangddang.dto.third.JudgeContextDto;
import com.demoday.ddangddangddang.dto.third.FinalJudgmentRequestDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.CaseRepository;
import com.demoday.ddangddangddang.repository.VoteRepository;
import com.demoday.ddangddangddang.service.ChatGptService;
import com.demoday.ddangddangddang.service.third.FinalJudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeFacade {

    private final FinalJudgeService finalJudgeService;
    private final CaseRepository caseRepository;
    private final VoteRepository voteRepository;
    private final JudgmentAsyncExecutor judgmentAsyncExecutor;

    public ApiResponse<String> requestFinalJudge(Long caseId, Long userId) {
        // 1. Read-only 트랜잭션으로 데이터 준비
        JudgeContextDto context = finalJudgeService.prepareJudgeContext(caseId, userId);
        Long voteA = voteRepository.countByaCase_IdAndType(caseId, DebateSide.A);
        Long voteB = voteRepository.countByaCase_IdAndType(caseId,DebateSide.B);

        FinalJudgmentRequestDto voteDto = FinalJudgmentRequestDto.builder()
                .votesA(voteA)
                .votesB(voteB)
                .build();

        // 2. 비동기 작업 시작 (여기서 더 이상 안 기다림)
        judgmentAsyncExecutor.processAsyncJudgment(caseId, voteDto, context);

        // 3. 클라이언트에게는 바로 응답
        return ApiResponse.onSuccess("판결 요청이 접수되었습니다.", "JUDGMENT_IN_PROGRESS");
    }

}
