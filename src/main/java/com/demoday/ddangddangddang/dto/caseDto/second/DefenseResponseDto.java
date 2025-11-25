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
    private String authorProfileUrl;
    private String authorRank;
    private DebateSide side;
    private String content;
    private Integer likesCount;
    private Boolean isLikedByMe;
    private long rebuttalCount; // 이 변론에 달린 반론(대댓글)의 총 개수
    private Boolean isBlind;

    public static DefenseResponseDto fromEntity(Defense defense, boolean isLikedByMe, long rebuttalCount) {
        // 블라인드 여부에 따른 콘텐츠 마스킹 로직
        String displayContent = defense.getIsBlind() ? "블라인드 처리된 내용입니다." : defense.getContent();

        return DefenseResponseDto.builder()
                .defenseId(defense.getId())
                .authorNickname(defense.getUser().getNickname())
                .authorProfileUrl(defense.getUser().getProfileImageUrl())
                .authorRank(defense.getUser().getRank().getDisplayName())
                .side(defense.getType())
                .content(displayContent) // 마스킹된 내용 주입
                .likesCount(defense.getLikesCount())
                .isLikedByMe(isLikedByMe)
                .rebuttalCount(rebuttalCount)
                .isBlind(defense.getIsBlind()) // 상태값 추가
                .build();
    }
}