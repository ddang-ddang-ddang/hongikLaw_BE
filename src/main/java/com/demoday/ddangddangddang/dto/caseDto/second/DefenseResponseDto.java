package com.demoday.ddangddangddang.dto.caseDto.second;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DefenseResponseDto {
    private Long defenseId;
    private String authorNickname;
    private String authorRank;
    private DebateSide side;
    private String content;
    private Integer likesCount;
    private Boolean isLikedByMe;
    private long rebuttalCount; // 이 변론에 달린 반론(대댓글)의 총 개수

    public static DefenseResponseDto fromEntity(Defense defense, boolean isLikedByMe, long rebuttalCount) {
        return DefenseResponseDto.builder()
                .defenseId(defense.getId())
                .authorNickname(defense.getUser().getNickname())
                .authorRank(defense.getUser().getRank().getDisplayName())
                .side(defense.getType())
                .content(defense.getContent())
                .likesCount(defense.getLikesCount())
                .isLikedByMe(isLikedByMe)
                .rebuttalCount(rebuttalCount)
                .build();
    }
}