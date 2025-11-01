package com.demoday.ddangddangddang.dto.home;

import com.demoday.ddangddangddang.domain.enums.CaseResult;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CaseOnResponseDto {
    private Long caseId;
    private String title;
    private CaseStatus status; //1,2,3차 완결
    private List<String> mainArguments; //초기 의견 제목들
}
