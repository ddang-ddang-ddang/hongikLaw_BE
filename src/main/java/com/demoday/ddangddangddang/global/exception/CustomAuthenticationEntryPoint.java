package com.demoday.ddangddangddang.global.exception;

import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j(topic = "AuthenticationEntryPoint")
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.error("인증되지 않은 사용자 접근: {}", authException.getMessage());

        // GeneralErrorCode에서 MISSING_AUTH_INFO 코드를 가져옵니다.
        GeneralErrorCode errorCode = GeneralErrorCode.MISSING_AUTH_INFO;

        //ApiResponse 객체 생성
        ApiResponse<Void> apiResponse = ApiResponse.onFailure(errorCode, authException.getMessage());

        // 응답 설정
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // JSON으로 변환하여 응답 body에 쓰기
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}