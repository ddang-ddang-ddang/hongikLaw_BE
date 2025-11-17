package com.demoday.ddangddangddang.controller.third;

import com.demoday.ddangddangddang.dto.third.FinalJudgmentRequestDto;
import com.demoday.ddangddangddang.dto.third.JudgementDetailResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.third.FinalJudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FINALJUDGE API", description = "최종 판결 API -by 황신애")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/final/judge")
public class FinalJudgeController {
    private final FinalJudgeService finalJudgeService;

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
}
