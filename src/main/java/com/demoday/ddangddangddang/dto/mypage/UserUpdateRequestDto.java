package com.demoday.ddangddangddang.dto.mypage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserUpdateRequestDto {
    private String nickname;
    private String profileImageUrl;
    private String email;
}
