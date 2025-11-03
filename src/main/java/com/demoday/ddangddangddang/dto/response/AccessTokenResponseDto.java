package com.demoday.ddangddangddang.dto.response;

import lombok.Getter;

@Getter
public class AccessTokenResponseDto {
    private final String accessToken;

    public AccessTokenResponseDto(String accessToken) {
        this.accessToken = accessToken;
    }
}