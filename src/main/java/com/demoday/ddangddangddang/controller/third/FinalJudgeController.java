package com.demoday.ddangddangddang.controller.third;

import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import com.demoday.ddangddangddang.dto.third.FinalJudgmentRequestDto;
import com.demoday.ddangddangddang.dto.third.JudgementDetailResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.cases.DebateService;
import com.demoday.ddangddangddang.service.third.FinalJudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FINALJUDGE API", description = "최종 판결 API -by 황신애")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/final/judge")
public class FinalJudgeController {
    private final FinalJudgeService finalJudgeService;
    private final DebateService debateService;

    @SecurityRequirement(name = "JWT TOKEN")
    @Operation(summary = "판결문 생성", description = "사건에 대한 최종판결문을 생성합니다")
    @PostMapping("/{caseId}")
    public ApiResponse<Long> createFinalJudge(
            @PathVariable Long caseId,
            @RequestBody FinalJudgmentRequestDto voteDto, @AuthenticationPrincipal UserDetailsImpl user
            ) {
        Long userId = user.getUser().getId();
        return finalJudgeService.createJudge(caseId, voteDto, userId);
    }

    @Operation(summary = "판결문 조회", description = "최종 판결문을 조회합니다")
    @GetMapping("/{caseId}")
    public ApiResponse<JudgementDetailResponseDto> getFinalJudgment(@PathVariable Long caseId){
        return finalJudgeService.getFinalJudgemnet(caseId);
    }

    @Operation(summary = "최종 판결 히스토리 조회 (아카이브)", description = "최종심이 진행되며 아카이빙된 모든 '판결 스냅샷' 목록을 오래된 순으로 조회합니다.")
    @GetMapping("/{caseId}/history") // [✅ 신규 API]
    public ApiResponse<List<JudgmentResponseDto>> getFinalJudgmentHistory(@PathVariable Long caseId) {
        return finalJudgeService.getJudgmentHistory(caseId);
    }
}
