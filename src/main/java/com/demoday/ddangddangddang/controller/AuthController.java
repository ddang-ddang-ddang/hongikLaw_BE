package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.auth.LoginRequestDto;
import com.demoday.ddangddangddang.dto.auth.SignupRequestDto;
import com.demoday.ddangddangddang.dto.auth.TokenRefreshRequestDto;
import com.demoday.ddangddangddang.dto.auth.AccessTokenResponseDto;
import com.demoday.ddangddangddang.dto.auth.LoginResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse; // <-- 프로젝트의 ApiResponse 임포트
import com.demoday.ddangddangddang.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid @RequestBody SignupRequestDto requestDto
    ) {
        authService.signup(requestDto);

        // 프로젝트의 ApiResponse.onSuccess 사용
        ApiResponse<Void> response = ApiResponse.onSuccess("회원가입에 성공하였습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto requestDto
    ) {
        LoginResponseDto loginResponse = authService.login(requestDto);

        // 프로젝트의 ApiResponse.onSuccess 사용
        ApiResponse<LoginResponseDto> response = ApiResponse.onSuccess(
                "로그인에 성공하였습니다.",
                loginResponse // 로그인 성공 시 result에 토큰 정보 포함
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Access Token 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> refreshAccessToken(
            @Valid @RequestBody TokenRefreshRequestDto requestDto
    ) {
        AccessTokenResponseDto responseDto = authService.refreshAccessToken(requestDto);

        ApiResponse<AccessTokenResponseDto> response = ApiResponse.onSuccess(
                "액세스 토큰이 성공적으로 재발급되었습니다.",
                responseDto
        );
        return ResponseEntity.ok(response);
    }
}