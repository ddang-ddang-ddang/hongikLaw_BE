package com.demoday.ddangddangddang.dto.mypage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserUpdateResponseDto {
    private String nickname;
    private String profileImage;
    private String email;
}
