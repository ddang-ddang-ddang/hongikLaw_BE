package com.demoday.ddangddangddang.controller.auth;

import com.demoday.ddangddangddang.dto.auth.AccessTokenResponseDto;
import com.demoday.ddangddangddang.dto.auth.EmailVerificationRequestDto;
import com.demoday.ddangddangddang.dto.auth.EmailSendRequestDto;
import com.demoday.ddangddangddang.dto.auth.LoginRequestDto;
import com.demoday.ddangddangddang.dto.auth.SignupRequestDto;
import com.demoday.ddangddangddang.dto.auth.TokenRefreshRequestDto;
import com.demoday.ddangddangddang.dto.auth.LoginResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.service.auth.AuthService;
import com.demoday.ddangddangddang.service.auth.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth API", description = "사용자 인증 (회원가입, 로그인, 토큰 재발급) API - by 최우혁")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Operation(summary = "회원가입", description = "이메일, 닉네임, 비밀번호로 회원가입을 진행합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"회원가입에 성공하였습니다.\",\"result\":null,\"error\":null}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패 (예: 이메일 형식 오류, 닉네임 길이)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"REQ_4002\",\"message\":\"파라미터 형식이 잘못되었습니다.\",\"result\":null,\"error\":\"[emailAuthCode] 이메일 인증번호를 입력해주세요.\"}")                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 미인증 (에러 코드 AUTH_4003)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"AUTH_4003\",\"message\":\"이메일 인증이 완료되지 않았습니다.\",\"result\":null,\"error\":\"이메일 인증이 필요합니다.\"}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복 (에러 코드 AUTH_4001)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"AUTH_4001\",\"message\":\"중복되는 아이디가 존재합니다.\",\"result\":null,\"error\":\"이미 존재하는 이메일입니다.\"}")
                    )
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid @RequestBody SignupRequestDto requestDto
    ) {
        authService.signup(requestDto);
        ApiResponse<Void> response = ApiResponse.onSuccess("회원가입에 성공하였습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "이메일 인증번호 발송", description = "회원가입을 위해 이메일로 인증번호를 발송합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증번호 발송 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"인증번호가 성공적으로 발송되었습니다.\",\"result\":null,\"error\":null}")
                    )
            )
    })
    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody EmailSendRequestDto requestDto
    ) {
        emailService.sendVerificationCode(requestDto.getEmail());
        ApiResponse<Void> response = ApiResponse.onSuccess("인증번호가 성공적으로 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 인증번호 검증", description = "회원가입 전, 이메일로 발송된 인증번호를 검증합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 인증 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"이메일 인증에 성공하였습니다.\",\"result\":null,\"error\":null}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증번호 불일치 (에러 코드 AUTH_4002)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"AUTH_4002\",\"message\":\"이메일 인증번호가 유효하지 않습니다.\",\"result\":null,\"error\":\"인증번호가 일치하지 않습니다.\"}")
                    )
            )
    })
    @PostMapping("/email/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(
            @Valid @RequestBody EmailVerificationRequestDto requestDto
    ) {
        emailService.verifyCode(requestDto.getEmail(), requestDto.getCode());
        ApiResponse<Void> response = ApiResponse.onSuccess("이메일 인증에 성공하였습니다.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인을 진행하고 토큰을 발급합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"로그인에 성공하였습니다.\",\"result\":{\"accessToken\":\"ey...\",\"refreshToken\":\"ey...\"},\"error\":null}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 실패 (아이디 또는 비밀번호 불일치)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"AUTH_4012\",\"message\":\"올바르지 않은 아이디, 혹은 비밀번호입니다.\",\"result\":null,\"error\":\"이메일 또는 비밀번호가 일치하지 않습니다.\"}")
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto requestDto,
            HttpServletResponse response // [변경] 쿠키를 담기 위해 response 객체 주입
    ) {
        LoginResponseDto loginResponse = authService.login(requestDto);

        // [추가] 리프레시 토큰을 쿠키로 생성
        ResponseCookie cookie = ResponseCookie.from("refresh_token", loginResponse.getRefreshToken())
                .path("/")
                .sameSite("None")        // [중요] 서브 도메인 간(api <-> www) 공유를 위해 None
                .httpOnly(true)          // [중요] 자바스크립트 접근 불가 (XSS 방지)
                .secure(true)            // [중요] HTTPS 필수 (SameSite=None 쓰려면 필수)
                .domain(".ddangx3.site")  // [매우 중요] 본인 도메인으로 수정하세요 (앞에 점 필수!)
                .maxAge(60 * 60 * 24 * 7) // 7일 (본인 정책에 맞게 수정)
                .build();

        // 응답 헤더에 쿠키 추가
        response.addHeader("Set-Cookie", cookie.toString());

        ApiResponse<LoginResponseDto> apiResponse = ApiResponse.onSuccess(
                "로그인에 성공하였습니다.",
                loginResponse
        );
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "AccessToken 재발급", description = "RefreshToken을 사용하여 새 AccessToken을 발급받습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"액세스 토큰이 성공적으로 재발급되었습니다.\",\"result\":{\"accessToken\":\"ey...\"},\"error\":null}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 재발급 실패 (유효하지 않거나 만료된 RefreshToken)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"AUTH_4012\",\"message\":\"유효하지 않은 토큰입니다.\",\"result\":null,\"error\":\"유효하지 않은 리프레시 토큰입니다.\"}")
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> refreshAccessToken(
            // [변경] RequestBody 대신 쿠키에서 직접 꺼냄
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) {
        // 쿠키가 없는 경우 예외 처리
        if (refreshToken == null) {
            // GeneralErrorCode.INVALID_TOKEN 혹은 적절한 에러 코드로 변경하세요
            throw new GeneralException(GeneralErrorCode.INVALID_TOKEN, "쿠키에 리프레시 토큰이 존재하지 않습니다.");
        }

        // 서비스 계층은 DTO를 받도록 되어 있으므로, DTO를 생성해서 값을 넣어줌
        TokenRefreshRequestDto requestDto = new TokenRefreshRequestDto();
        requestDto.setRefreshToken(refreshToken); // DTO에 Setter가 있어야 합니다!

        AccessTokenResponseDto responseDto = authService.refreshAccessToken(requestDto);

        ApiResponse<AccessTokenResponseDto> response = ApiResponse.onSuccess(
                "액세스 토큰이 성공적으로 재발급되었습니다.",
                responseDto
        );
        return ResponseEntity.ok(response);
    }

}