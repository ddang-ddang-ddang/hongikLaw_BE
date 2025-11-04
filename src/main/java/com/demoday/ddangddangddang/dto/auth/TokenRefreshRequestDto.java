package com.demoday.ddangddangddang.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRefreshRequestDto {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email
    private String email;

    @NotBlank(message = "리프레시 토큰을 입력해주세요.")
    private String refreshToken;
}