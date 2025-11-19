package com.demoday.ddangddangddang.controller.report;

import com.demoday.ddangddangddang.dto.report.ReportRequestDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report API", description = "신고하기 API")
@SecurityRequirement(name = "JWT TOKEN")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "변론/반론 신고하기", description = "부적절한 변론(DEFENSE) 또는 반론(REBUTTAL)을 신고합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReportRequestDto requestDto
    ) {
        reportService.createReport(userDetails.getUser().getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("신고가 성공적으로 접수되었습니다."));
    }
}