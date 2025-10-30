package com.demoday.ddangddangddang.domain.mypage.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordResponseDto {
    private Long id;
    private Integer winCnt;
    private Integer lossCnt;
}
