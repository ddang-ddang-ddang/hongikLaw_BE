package com.demoday.ddangddangddang.controller.cases;

import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.dto.caseDto.party.CasePendingResponseDto;
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

import java.util.List;

@Tag(name = "Case (1차 재판) API", description = "사건 생성(1차 재판) 관련 API - by 최우혁")
@SecurityRequirement(name = "JWT TOKEN")
@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    // --- [ 1차 재판 API (기존) ] ---

    @Operation(summary = "1차 재판(초심) 생성 (솔로/VS 모드)", description = "mode: SOLO(솔로), PARTY(VS모드)")
    @SecurityRequirement(name = "JWT TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<CaseResponseDto>> createCase(
            @Valid @RequestBody CaseRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseResponseDto responseDto = caseService.createCase(requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("사건이 성공적으로 생성되었습니다.", responseDto));
    }

    // VS 모드 대기 목록 조회
    @Operation(summary = "VS 모드 매칭 대기 목록 조회", description = "매칭 대기 중(PENDING)인 사건 목록을 조회합니다.")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<CasePendingResponseDto>>> getPendingCases() {
        List<CasePendingResponseDto> responseDto = caseService.getPendingCases();
        return ResponseEntity.ok(ApiResponse.onSuccess("매칭 대기 중인 사건 목록 조회 성공", responseDto));
    }

    // VS 모드 1차 입장문 제출 (참여)
    @Operation(summary = "VS 모드 1차 입장문 제출 (참여)", description = "PENDING 상태인 사건에 B측 입장문을 제출하여 참여합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    @PostMapping("/{caseId}/arguments")
    public ResponseEntity<ApiResponse<Void>> createInitialArgument(
            @PathVariable Long caseId,
            @Valid @RequestBody ArgumentInitialRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        caseService.createInitialArgument(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("B측 입장문이 성공적으로 등록되었습니다. 1차 재판이 시작됩니다."));
    }

    @Operation(summary = "사건 상세 조회 (1차 입장문 포함)")
    @SecurityRequirement(name = "JWT TOKEN")
    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CaseDetailResponseDto>> getCaseDetail(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseDetailResponseDto responseDto = caseService.getCaseDetail(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("사건 상세 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "1차 재판(초심) AI 판결 결과 조회")
    @SecurityRequirement(name = "JWT TOKEN")
    @GetMapping("/{caseId}/judgment")
    public ResponseEntity<ApiResponse<JudgmentResponseDto>> getJudgment(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        JudgmentResponseDto responseDto = caseService.getJudgment(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("1차 판결 결과 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "1차 재판 상태 변경 (종료)", description = "1차 판결 확인 후, 사건을 완전히 종료(DONE)시킵니다.")
    @SecurityRequirement(name = "JWT TOKEN")
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