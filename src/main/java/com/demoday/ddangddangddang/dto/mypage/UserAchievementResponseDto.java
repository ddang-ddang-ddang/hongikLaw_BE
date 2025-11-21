package com.demoday.ddangddangddang.dto.mypage;

import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserAchievementResponseDto {
    private Long userId;
    private Long achievementId;
    private String achievementName;
    private String achievementDescription;
    private String achievementIconUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime achievementTime;
}
