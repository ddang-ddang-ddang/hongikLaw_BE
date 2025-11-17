package com.demoday.ddangddangddang.dto.caseDto.second;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AppealRequestDto {
    @NotNull(message = "마감 시간을 설정해야 합니다.")
    @Future(message = "마감 시간은 현재 시간 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
}
