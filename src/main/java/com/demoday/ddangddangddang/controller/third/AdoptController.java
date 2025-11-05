package com.demoday.ddangddangddang.controller.third;

import com.demoday.ddangddangddang.dto.third.AdoptRequestDto;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.third.AdoptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/final/adopt")
@Tag(name = "Adopt API", description = "채택 관련 API - by 황신애")
public class AdoptController {
    private final AdoptService adoptService;

    @GetMapping("/{caseId}/best")
    public ApiResponse<AdoptResponseDto> getOpinionBest(@AuthenticationPrincipal UserDetailsImpl user,@PathVariable Long caseId) {
        Long userId = user.getUser().getId();
        return adoptService.getOpinionBest(userId,caseId);
    }

    @PostMapping("/{caseId}")
    public ApiResponse<String> adoptCase(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable Long caseId, @RequestBody AdoptRequestDto adoptRequestDto) {
        Long userId = user.getUser().getId();
        return adoptService.createAdopt(userId,caseId,adoptRequestDto);
    }

    @GetMapping("/{caseId}")
    public ApiResponse<AdoptResponseDto> getAdoptCase(@PathVariable Long caseId) {
        return adoptService.getAdopt(caseId);
    }
}
