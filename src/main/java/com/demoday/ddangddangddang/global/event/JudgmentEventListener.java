package com.demoday.ddangddangddang.global.event;

import com.demoday.ddangddangddang.service.cases.DebateService; // ⭐️ 신규 서비스 Import
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JudgmentEventListener {
/*
    // private final CaseService caseService; // ⭐️ (주의) 순환 참조 방지
    private final DebateService debateService; // ⭐️ 호출할 서비스 변경

    @Async
    @EventListener
    public void handleUpdateJudgment(UpdateJudgmentEvent event) {
        // 비동기로 AI 판결 업데이트 로직 호출
        // caseService.updateFinalJudgment(event.getCaseId()); // ⭐️ 변경 전
        debateService.updateFinalJudgment(event.getCaseId()); // ⭐️ 변경 후
    }

 */
}