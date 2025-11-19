package com.demoday.ddangddangddang.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportReason {
    PROFANITY("욕설/비하 발언"),
    SLANDER("인신공격/명예훼손"),
    SPAM("도배/스팸"),
    ADVERTISEMENT("상업적 광고"),
    OBSCENE("음란성/부적절한 홍보"),
    OTHER("기타");

    private final String description;
}