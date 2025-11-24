package com.demoday.ddangddangddang.global.exception.handler;

import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.BaseErrorCode;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class ExceptionAdvice {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(GeneralException e) {
        log.warn("CustomException: {}", e.getCode().getMessage());
        BaseErrorCode code = e.getCode();
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code, e.getMessage()));
    }

    // @Valid 바디 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        var errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> String.format("[%s] %s (입력값: %s)",
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()))
                .toList();

        log.warn("Validation failed: {}", errors);

        BaseErrorCode code = GeneralErrorCode.INVALID_PARAMETER;
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(code, errors));
    }

    /**
     * DB 제약조건 위반 시 (중복 신고 등) 발생하는 예외 처리
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("Data Integrity Violation: {}", e.getMessage());
        // 이미 정의된 에러 코드 활용 (REPORT_ALREADY_EXISTS)
        BaseErrorCode code = GeneralErrorCode.REPORT_ALREADY_EXISTS;

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(code, "이미 처리된 요청이거나 중복된 데이터입니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleJsonErrors(HttpMessageNotReadableException e) {
        log.warn("JSON Parse Error: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(GeneralErrorCode.INVALID_PARAMETER, "입력값이 잘못되었습니다. (JSON 형식을 확인해주세요)"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.warn("Exception: {}", e.getMessage());
        BaseErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.onFailure(code, e.getMessage()));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        // SSE 타임아웃은 로그만 남기고 아무것도 반환하지 않음 (클라이언트가 이미 연결을 끊었거나 재연결 시도함)
        log.debug("SSE Timeout: {}", e.getMessage());
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        // 클라이언트가 연결을 끊은 경우, 응답을 보낼 필요가 없음
        log.debug("SSE Client Disconnected: {}", e.getMessage());
    }


}
