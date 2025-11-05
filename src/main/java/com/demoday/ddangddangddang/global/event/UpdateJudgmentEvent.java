package com.demoday.ddangddangddang.global.event;

import lombok.Getter;

// AI 판결 업데이트를 트리거하는 이벤트 객체
@Getter
public class UpdateJudgmentEvent {
    private final Long caseId;

    public UpdateJudgmentEvent(Long caseId) {
        this.caseId = caseId;
    }
}