package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.request.LoginRequestDto;
import com.demoday.ddangddangddang.dto.request.SignupRequestDto;
import com.demoday.ddangddangddang.dto.response.LoginResponseDto;
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
}