package com.demoday.ddangddangddang.dto.notice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponseDto {
    private Long caseId;
    private Long defenseId;
    private Long parentId;
    private Long rebuttalId;
    private String message;
    private String iconUrl;
}
