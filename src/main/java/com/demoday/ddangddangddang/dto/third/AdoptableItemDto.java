package com.demoday.ddangddangddang.dto.third;

import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 제외
public class AdoptableItemDto {
    // 공통 필드
    private ContentType itemType; // "DEFENSE" 또는 "REBUTTAL"
    private Long id; // 변론 ID 또는 반론 ID
    private Long caseId;
    private Long userId;
    private DebateSide debateSide;
    private String content;
    private int likeCount;

    // --- 반론(Rebuttal) 전용 필드 ---
    private Long defenseId; // 이 반론이 속한 변론의 ID
    private Long parentId;
    private String parentContent;
}
