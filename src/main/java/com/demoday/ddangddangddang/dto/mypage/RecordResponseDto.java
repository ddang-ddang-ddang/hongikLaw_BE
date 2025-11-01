package com.demoday.ddangddangddang.dto.mypage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordResponseDto {
    private Long id;
    private Integer winCnt;
    private Integer lossCnt;
}
