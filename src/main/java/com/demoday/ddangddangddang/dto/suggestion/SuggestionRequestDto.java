package com.demoday.ddangddangddang.dto.suggestion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SuggestionRequestDto {

    @NotBlank(message = "건의 내용을 입력해주세요.")
    @Schema(description = "건의 사항 내용", example = "다크 모드를 추가해주세요!")
    private String content;
}