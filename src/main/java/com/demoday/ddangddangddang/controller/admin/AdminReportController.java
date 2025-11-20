package com.demoday.ddangddangddang.controller.admin;

import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Report API", description = "운영자용 신고 관리 API - by 최우혁")
public class AdminReportController {

    private final ReportService reportService;

    // TODO: 실제 운영 시에는 @PreAuthorize("hasRole('ADMIN')") 등으로 권한 체크 필요
    @Operation(summary = "허위 신고 삭제", description = "운영자가 허위 신고로 판단한 경우 신고 내역을 삭제합니다. (삭제 후 누적 신고 수가 3회 미만이 되면 블라인드가 해제됩니다.)")
    @DeleteMapping("/{reportId}")
    public ApiResponse<Void> deleteFalseReport(@PathVariable Long reportId) {
        reportService.deleteReport(reportId);
        return ApiResponse.onSuccess("허위 신고가 삭제되었습니다.");
    }
}