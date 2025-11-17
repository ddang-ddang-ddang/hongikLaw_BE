package com.demoday.ddangddangddang.dto.caseDto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ArgumentInitialRequestDto {

    @NotBlank(message = "핵심 입장을 입력해주세요.")
    private String mainArgument;

    @NotBlank(message = "상세 근거를 입력해주세요.")
    private String reasoning;
}