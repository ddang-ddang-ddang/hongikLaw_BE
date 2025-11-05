package com.demoday.ddangddangddang.global.event;

import com.demoday.ddangddangddang.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JudgmentEventListener {

    private final CaseService caseService; // [주의] 순환 참조가 발생할 수 있음

    @Async // 이 메서드를 비동기 스레드에서 실행
    @EventListener
    public void handleUpdateJudgment(UpdateJudgmentEvent event) {
        // 비동기로 AI 판결 업데이트 로직 호출
        caseService.updateFinalJudgment(event.getCaseId());
    }
}