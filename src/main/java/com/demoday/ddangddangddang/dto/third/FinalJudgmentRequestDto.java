package com.demoday.ddangddangddang.dto.third;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalJudgmentRequestDto {
    private Long votesA;
    private Long votesB;
}
