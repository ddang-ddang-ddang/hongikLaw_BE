package com.demoday.ddangddangddang.dto.mypage;

import com.demoday.ddangddangddang.domain.enums.Rank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankResponseDto {
    private Long id;
    private Rank rank;
    private Long exp;
}
