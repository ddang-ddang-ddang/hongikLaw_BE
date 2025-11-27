package com.demoday.ddangddangddang.dto.home;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseSimpleDto {
    private Long caseId;
    private String title;
    private List<String> mainArguments;
    private Integer participateCnt;
    private Boolean isAd;
}
