package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.CaseParticipation;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.third.JudgeContextDto;
import com.demoday.ddangddangddang.dto.third.FinalJudgmentRequestDto;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.CaseParticipationRepository;
import com.demoday.ddangddangddang.service.ChatGptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgmentAsyncExecutor {

    private final FinalJudgeService finalJudgeService;
    private final ChatGptService chatGptService;
    private final SseEmitters sseEmitters;
    private final CaseParticipationRepository caseParticipationRepository;

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

            // B. DB 저장 및 정산
            Long judgmentId = finalJudgeService.saveJudgeResultAndSettle(caseId, aiResult);

            // C. SSE 알림
            // 1. 해당 사건의 참여자 목록 조회
            List<CaseParticipation> participants = caseParticipationRepository.findByaCase(context.getACase());

            // 2. 참여자들에게 각각 알림 전송
            for (CaseParticipation participation : participants) {
                Long userId = participation.getUser().getId();

                // 유저별 연결에 'judgment_complete' 이벤트 전송
                sseEmitters.sendNotification(
                        userId,
                        "judgment_complete",
                        String.valueOf(judgmentId) // 데이터로 판결문 ID 전송
                );
            }

        } catch (Exception e) {
                // 에러 알림도 동일하게 참여자들에게 전송
                List<CaseParticipation> participant = caseParticipationRepository.findByaCase(context.getACase());
                for (CaseParticipation p : participant) {
                    sseEmitters.sendNotification(p.getUser().getId(), "judgment_error", "판결 생성 중 오류가 발생했습니다.");
            }
        }
    }
}

