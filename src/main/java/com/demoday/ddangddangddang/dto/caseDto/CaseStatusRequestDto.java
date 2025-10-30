package com.demoday.ddangddangddang.dto.caseDto;

import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CaseStatusRequestDto {
    @NotNull(message = "변경할 상태를 입력해주세요.")
    private CaseStatus status; // (CaseStatus Enum 사용: FIRST, SECOND, THIRD, DONE)
}