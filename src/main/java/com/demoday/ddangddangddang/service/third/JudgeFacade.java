package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.third.JudgeContextDto;
import com.demoday.ddangddangddang.dto.third.FinalJudgmentRequestDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.VoteRepository;
import com.demoday.ddangddangddang.service.ChatGptService;
import com.demoday.ddangddangddang.service.third.FinalJudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeFacade {

    private final FinalJudgeService finalJudgeService;
    private final ChatGptService chatGptService;
    private final VoteRepository voteRepository;
    private final SseEmitters sseEmitters;

    // 1. 동기: 클라이언트 요청 접수 및 비동기 처리 시작
    public ApiResponse<String> requestFinalJudge(Long caseId, FinalJudgmentRequestDto voteDto, Long userId) {
        // Read-only 트랜잭션으로 데이터 준비
        JudgeContextDto context = finalJudgeService.prepareJudgeContext(caseId, userId);

        // 비동기 작업 시작
        processAsyncJudgment(caseId, voteDto, context);

        return ApiResponse.onSuccess("판결 요청이 접수되었습니다.", "JUDGMENT_IN_PROGRESS");
    }

    // 2. 비동기: AI 호출 -> DB 저장 -> SSE 알림
    @Async
    public void processAsyncJudgment(Long caseId, FinalJudgmentRequestDto voteDto, JudgeContextDto context) {
        long startTime = System.currentTimeMillis();
        log.info("[Async] 판결 프로세스 시작: caseId={}", caseId);
        try {
            // A. AI 호출 (병목 구간)
            long apiCallStart = System.currentTimeMillis();
            AiJudgmentDto aiResult = chatGptService.requestFinalJudgment(context.getACase(),
                    context.getAdoptedDefenses(),
                    context.getAdoptedRebuttals(),
                    voteDto.getVotesA(),
                    voteDto.getVotesB());
            long apiCallEnd = System.currentTimeMillis();
            log.warn("⏱️ [측정] ChatGPT API 호출 시간: {}ms", apiCallEnd - apiCallStart); // API 호출 시간 측정

            // B. DB 저장 및 정산
            long dbStart = System.currentTimeMillis();
            Long judgmentId = finalJudgeService.saveJudgeResultAndSettle(caseId, aiResult);
            long dbEnd = System.currentTimeMillis();
            log.warn("⏱️ [측정] DB 저장 및 정산 시간: {}ms", dbEnd - dbStart); // DB 정산 시간 측정

            // C. SSE 알림 전송
            sseEmitters.send(caseId, judgmentId);

            long endTime = System.currentTimeMillis();
            log.info("[Async] 총 비동기 작업 시간: {}ms", endTime - startTime); // 총 비동기 작업 시간

        } catch (Exception e) {
            log.error("[Async] 판결 프로세스 실패: caseId={}, error={}", caseId, e.getMessage(), e);
            sseEmitters.sendError(caseId, "판결 생성 중 오류가 발생했습니다.");
        }
    }
}
