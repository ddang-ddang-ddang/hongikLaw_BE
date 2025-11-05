package com.demoday.ddangddangddang.dto.caseDto.second;

import com.demoday.ddangddangddang.domain.enums.DebateSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DefenseRequestDto {
    @NotNull(message = "입장을 선택해주세요.")
    private DebateSide side; // A 또는 B

    @NotBlank(message = "변론 내용을 입력해주세요.")
    private String content;
}