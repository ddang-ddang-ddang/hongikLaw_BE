package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.caseDto.CaseRequestDto;
import com.demoday.ddangddangddang.dto.caseDto.CaseResponseDto;
import com.demoday.ddangddangddang.dto.caseDto.CaseStatusRequestDto;
import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl; // UserDetailsImpl 임포트
import com.demoday.ddangddangddang.service.CaseService;
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

    /**
     * 1차 재판(초심) 결과 조회
     */
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