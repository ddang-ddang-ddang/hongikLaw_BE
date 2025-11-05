package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.auth.AccessTokenResponseDto;
import com.demoday.ddangddangddang.dto.auth.LoginRequestDto;
import com.demoday.ddangddangddang.dto.auth.SignupRequestDto;
import com.demoday.ddangddangddang.dto.auth.TokenRefreshRequestDto;
import com.demoday.ddangddangddang.dto.auth.LoginResponseDto; // [수정] LoginResponseDto 경로가 dto/response에 있습니다.
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth API", description = "사용자 인증 (회원가입, 로그인, 토큰 재발급) API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"REQ_4002\",\"message\":\"파라미터 형식이 잘못되었습니다.\",\"result\":null,\"error\":\"[nickname] 닉네임은 2자 이상 10자 이하로 입력해주세요.\"}")
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
            @Valid @RequestBody LoginRequestDto requestDto
    ) {
        LoginResponseDto loginResponse = authService.login(requestDto);
        ApiResponse<LoginResponseDto> response = ApiResponse.onSuccess(
                "로그인에 성공하였습니다.",
                loginResponse
        );
        return ResponseEntity.ok(response);
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