package com.demoday.ddangddangddang.dto.home;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CaseSimpleDto {
    private Long caseId;
    private String title;
    private List<String> mainArguments;
}
