package com.demoday.ddangddangddang.dto.third;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class JudgeContextDto {
    private final Case aCase;
    private final List<Defense> adoptedDefenses;
    private final List<Rebuttal> adoptedRebuttals;
}
