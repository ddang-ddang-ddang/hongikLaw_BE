package com.demoday.ddangddangddang.dto.caseDto.second;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
public class AppealRequestDto {

    @NotNull(message = "마감 시간을 설정해야 합니다.")
    @Min(value = 1, message = "마감 시간은 1시간 이상이어야 합니다.") // 1시간, 24시간 등
    @Schema(description = "마감까지 남은 시간 (시간 단위)", example = "24")
    private Integer hoursToAdd;
}
