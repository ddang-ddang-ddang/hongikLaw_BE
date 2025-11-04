package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl; // UserDetailsImpl 임포트
import com.demoday.ddangddangddang.service.CaseService;
import io.swagger.v3.oas.annotations.Operation; // <-- Operation 임포트 추가
import io.swagger.v3.oas.annotations.responses.ApiResponses; // <-- ApiResponses 임포트 추가
import io.swagger.v3.oas.annotations.media.Content; // <-- Content 임포트 추가
import io.swagger.v3.oas.annotations.media.Schema; // <-- Schema 임포트 추가
import io.swagger.v3.oas.annotations.media.ExampleObject; // <-- ExampleObject 임포트 추가
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // @AuthenticationPrincipal 임포트
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    /**
     * 1차 재판(초심) 생성 (솔로 모드)
     */
    @Operation(summary = "1차 재판(초심) 생성 (솔로 모드)")
    @PostMapping
    public ResponseEntity<ApiResponse<CaseResponseDto>> createCase(
            @Valid @RequestBody CaseRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails // [중요] 로그인한 사용자 정보 가져오기
    ) {
        CaseResponseDto responseDto = caseService.createCase(requestDto, userDetails.getUser());

        ApiResponse<CaseResponseDto> response = ApiResponse.onSuccess(
                "사건이 성공적으로 생성되었습니다.",
                responseDto
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- [ 1. 이 API 엔드포인트를 새로 추가 ] ---
    @Operation(summary = "사건 상세 조회 (1차 입장문 포함)", description = "특정 사건의 상세 내용과 1차 입장문을 조회합니다. (인증 필요)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사건 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\n  \"isSuccess\": true,\n  \"code\": \"COMMON2000\",\n  \"message\": \"사건 상세 조회에 성공하였습니다.\",\n  \"result\": {\n    \"caseId\": 101,\n    \"title\": \"탕수육은 부먹인가 찍먹인가\",\n    \"status\": \"FIRST\",\n    \"mode\": \"SOLO\",\n    \"argumentA\": {\n      \"mainArgument\": \"바삭함이 생명이다\",\n      \"reasoning\": \"소스가 부어지면...\",\n      \"authorId\": 1\n    },\n    \"argumentB\": {\n      \"mainArgument\": \"원래 부어먹는 음식\",\n      \"reasoning\": \"소스가 스며들어야...\",\n      \"authorId\": 1\n    }\n  },\n  \"error\": null\n}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 사건을 찾을 수 없습니다.", content = @Content)
    })
    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CaseDetailResponseDto>> getCaseDetail(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseDetailResponseDto responseDto = caseService.getCaseDetail(caseId, userDetails.getUser());
        ApiResponse<CaseDetailResponseDto> response = ApiResponse.onSuccess(
                "사건 상세 조회에 성공하였습니다.",
                responseDto
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 1차 재판(초심) 결과 조회
     */
    @Operation(summary = "1차 재판(초심) 결과 조회")
    @GetMapping("/{caseId}/judgment")
    public ResponseEntity<ApiResponse<JudgmentResponseDto>> getJudgment(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        JudgmentResponseDto responseDto = caseService.getJudgment(caseId, userDetails.getUser());

        ApiResponse<JudgmentResponseDto> response = ApiResponse.onSuccess(
                "1차 판결 결과 조회에 성공하였습니다.",
                responseDto
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 1차 재판 상태 변경 (종료 또는 2차로)
     */
    @PatchMapping("/{caseId}/status")
    public ResponseEntity<ApiResponse<Void>> updateCaseStatus(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseStatusRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        caseService.updateCaseStatus(caseId, requestDto, userDetails.getUser());

        ApiResponse<Void> response = ApiResponse.onSuccess(
                "사건 상태가 성공적으로 변경되었습니다."
        );
        return ResponseEntity.ok(response);
    }
}