package com.demoday.ddangddangddang.dto.caseDto;

import com.demoday.ddangddangddang.domain.enums.CaseMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CaseRequestDto {

    @NotNull(message = "모드를 선택해주세요.")
    private CaseMode mode; // SOLO

    @NotBlank(message = "사건 설명을 입력해주세요.")
    private String title; // 상황 설명

    @NotBlank(message = "A측 입장을 입력해주세요.")
    private String argumentAMain; // A측 입장

    @NotBlank(message = "A측 근거를 입력해주세요.")
    private String argumentAReasoning; // A측 근거

    @NotBlank(message = "B측 입장을 입력해주세요.")
    private String argumentBMain; // B측 입장

    @NotBlank(message = "B측 근거를 입력해주세요.")
    private String argumentBReasoning; // B측 근거
}