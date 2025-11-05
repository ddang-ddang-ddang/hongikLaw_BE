package com.demoday.ddangddangddang.dto.caseDto.second;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VoteResultResponseDto {
    private long totalVotes;
    private long aCount;
    private long bCount;
    private double aPercent;
    private double bPercent;
}
