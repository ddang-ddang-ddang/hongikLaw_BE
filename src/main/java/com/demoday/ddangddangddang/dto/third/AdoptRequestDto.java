package com.demoday.ddangddangddang.dto.third;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class AdoptRequestDto {
    private List<Long> defenseId;
    private List<Long> rebuttalId;
}
