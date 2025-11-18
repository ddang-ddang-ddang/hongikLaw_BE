package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.third.JudgeContextDto;
import com.demoday.ddangddangddang.dto.third.FinalJudgmentRequestDto;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.service.ChatGptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgmentAsyncExecutor {

    private final FinalJudgeService finalJudgeService;
    private final ChatGptService chatGptService;
    private final SseEmitters sseEmitters;

    @Async
    public void processAsyncJudgment(Long caseId, FinalJudgmentRequestDto voteDto, JudgeContextDto context) {
        long startTime = System.currentTimeMillis();
        log.info("[Async] 판결 프로세스 시작: caseId={}", caseId);
        try {
            // A. AI 호출
            long apiCallStart = System.currentTimeMillis();
            AiJudgmentDto aiResult = chatGptService.requestFinalJudgment(
                    context.getACase(),
                    context.getAdoptedDefenses(),
                    context.getAdoptedRebuttals(),
                    voteDto.getVotesA(),
                    voteDto.getVotesB()
            );
            long apiCallEnd = System.currentTimeMillis();
            log.warn("⏱️ [측정] ChatGPT API 호출 시간: {}ms", apiCallEnd - apiCallStart);

            // B. DB 저장 및 정산
            long dbStart = System.currentTimeMillis();
            Long judgmentId = finalJudgeService.saveJudgeResultAndSettle(caseId, aiResult);
            long dbEnd = System.currentTimeMillis();
            log.warn("⏱️ [측정] DB 저장 및 정산 시간: {}ms", dbEnd - dbStart);

            // C. SSE 알림
            sseEmitters.send(caseId, judgmentId);

            long endTime = System.currentTimeMillis();
            log.info("[Async] 총 비동기 작업 시간: {}ms", endTime - startTime);

        } catch (Exception e) {
            log.error("[Async] 판결 프로세스 실패: caseId={}, error={}", caseId, e.getMessage(), e);
            sseEmitters.sendError(caseId, "판결 생성 중 오류가 발생했습니다.");
        }
    }
}

