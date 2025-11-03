package com.demoday.ddangddangddang.dto.mypage;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserAchievementResponseDto {
    private Long userId;
    private Long achievementId;
    private String achievementName;
    private String achievementDescription;
    private String achievementIconUrl;
    private LocalDateTime achievementTime;
}
