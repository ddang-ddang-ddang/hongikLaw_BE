package com.demoday.ddangddangddang.controller.cases;

import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.cases.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Case (1차 재판) API", description = "사건 생성(1차 재판) 관련 API - by 최우혁")
@SecurityRequirement(name = "JWT TOKEN")
@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    // --- [ 1차 재판 API (기존) ] ---

    @Operation(summary = "1차 재판(초심) 생성 (솔로 모드)")
    @PostMapping
    public ResponseEntity<ApiResponse<CaseResponseDto>> createCase(
            @Valid @RequestBody CaseRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseResponseDto responseDto = caseService.createCase(requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("사건이 성공적으로 생성되었습니다.", responseDto));
    }

    @Operation(summary = "사건 상세 조회 (1차 입장문 포함)")
    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CaseDetailResponseDto>> getCaseDetail(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseDetailResponseDto responseDto = caseService.getCaseDetail(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("사건 상세 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "1차 재판(초심) AI 판결 결과 조회")
    @GetMapping("/{caseId}/judgment")
    public ResponseEntity<ApiResponse<JudgmentResponseDto>> getJudgment(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        JudgmentResponseDto responseDto = caseService.getJudgment(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("1차 판결 결과 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "1차 재판 상태 변경 (종료)", description = "1차 판결 확인 후, 사건을 완전히 종료(DONE)시킵니다.")
    @PatchMapping("/{caseId}/status")
    public ResponseEntity<ApiResponse<Void>> updateCaseStatus(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseStatusRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        caseService.updateCaseStatus(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("사건 상태가 성공적으로 변경되었습니다."));
    }
}