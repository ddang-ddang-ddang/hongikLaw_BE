package com.demoday.ddangddangddang.domain.mypage.dto;

import com.demoday.ddangddangddang.domain.enums.CaseResult;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserArchiveResponseDto {
    private Long caseId;
    private String title;
    private CaseStatus status; //1,2,3차 완결
    private List<String> mainArguments; //초기 의견 제목들
    private CaseResult caseResult; //결과
}
