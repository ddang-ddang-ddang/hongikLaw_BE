package com.demoday.ddangddangddang.controller.third;

import com.demoday.ddangddangddang.dto.third.AdoptRequestDto;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.third.AdoptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/final/adopt")
public class AdoptController {
    private final AdoptService adoptService;

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
