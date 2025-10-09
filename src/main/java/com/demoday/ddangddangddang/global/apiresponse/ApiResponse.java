package com.demoday.ddangddangddang.global.apiresponse;

import com.demoday.ddangddangddang.global.code.BaseErrorCode;
import com.demoday.ddangddangddang.global.code.GeneralSuccessCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result", "error"})
//공통 api 응답 형식
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private Boolean isSuccess;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("result")
    private final T result;

    @JsonProperty("error")
    private Object error;

    // result가 있는 성공 응답
    public static <T> ApiResponse<T> onSuccess(String message,T result) {
        return new ApiResponse<>(true, GeneralSuccessCode.OK.getCode(), message, result, null);
    }

    // result가 없는 성공 응답
    public static <T> ApiResponse<T> onSuccess(String message) {
        return new ApiResponse<>(true, GeneralSuccessCode.OK.getCode(), message, null, null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> onFailure(BaseErrorCode errorCode, Object error) {
        return new ApiResponse<>(false,errorCode.getCode(),errorCode.getMessage(),null,error);
    }

}

