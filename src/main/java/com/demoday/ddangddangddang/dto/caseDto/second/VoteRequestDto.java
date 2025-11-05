package com.demoday.ddangddangddang.dto.caseDto.second;

import com.demoday.ddangddangddang.domain.enums.DebateSide;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VoteRequestDto {
    @NotNull(message = "투표할 입장을 선택해주세요.")
    private DebateSide choice; // A 또는 B
}