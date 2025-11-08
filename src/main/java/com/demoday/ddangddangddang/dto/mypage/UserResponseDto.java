package com.demoday.ddangddangddang.dto.mypage;

import com.demoday.ddangddangddang.domain.enums.Rank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {
    private String nickname;
    private String email;
    private Rank rank;
    private String profileImageUrl;
}
