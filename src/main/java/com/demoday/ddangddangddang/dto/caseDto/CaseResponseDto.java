package com.demoday.ddangddangddang.dto.caseDto;

import lombok.Getter;

@Getter
public class CaseResponseDto {
    private final Long caseId;

    public CaseResponseDto(Long caseId) {
        this.caseId = caseId;
    }
}