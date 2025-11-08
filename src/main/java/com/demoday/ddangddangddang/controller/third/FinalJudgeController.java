package com.demoday.ddangddangddang.controller.third;

import com.demoday.ddangddangddang.dto.third.FinalJudgmentRequestDto;
import com.demoday.ddangddangddang.dto.third.JudgementDetailResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.service.third.FinalJudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/final/judge")
public class FinalJudgeController {
    private final FinalJudgeService finalJudgeService;

    @PostMapping("/{caseId}")
    public ApiResponse<Long> createFinalJudge(
            @PathVariable Long caseId,
            @RequestBody FinalJudgmentRequestDto voteDto
    ) {
        // ⭐️ 서비스에 DTO 객체를 그대로 전달
        return finalJudgeService.createJudge(caseId, voteDto);
    }

    @GetMapping("/{caseId}")
    public ApiResponse<JudgementDetailResponseDto> getFinalJudgment(@PathVariable Long caseId){
        return finalJudgeService.getFinalJudgemnet(caseId);
    }
}
