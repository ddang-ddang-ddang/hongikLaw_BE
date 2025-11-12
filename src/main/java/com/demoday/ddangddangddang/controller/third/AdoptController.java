package com.demoday.ddangddangddang.controller.third;

import com.demoday.ddangddangddang.dto.third.AdoptRequestDto;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.third.AdoptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @SecurityRequirement(name = "JWT TOKEN")
    @Operation(summary = "좋아요 많은 순으로 조회", description = "현재 변론/반론을 좋아요순으로 조회합니다")
    @GetMapping("/{caseId}/best")
    public ApiResponse<AdoptResponseDto> getOpinionBest(@AuthenticationPrincipal UserDetailsImpl user,@PathVariable Long caseId) {
        Long userId = user.getUser().getId();
        return adoptService.getOpinionBest(userId,caseId);
    }

    @SecurityRequirement(name = "JWT TOKEN")
    @Operation(summary = "최종심으로 사건 상태 변경")
    @PostMapping("/{caseId}/third")
    public ApiResponse<Void> setThird(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable Long caseId){
        Long userId = user.getUser().getId();
        return adoptService.changeThird(userId,caseId);
    }

    @Operation(summary = "채택", description = "사용자가 프롬프트에 넣을 변론/반론을 선택합니다")
    @SecurityRequirement(name = "JWT TOKEN")
    @PostMapping("/{caseId}")
    public ApiResponse<String> adoptCase(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable Long caseId, @RequestBody AdoptRequestDto adoptRequestDto) {
        Long userId = user.getUser().getId();
        return adoptService.createAdopt(userId,caseId,adoptRequestDto);
    }

    @Operation(summary = "채택된 변론/반론 조회", description = "어떤 변론/반론들이 채택되었는지 조회합니다.")
    @GetMapping("/{caseId}")
    public ApiResponse<AdoptResponseDto> getAdoptCase(@PathVariable Long caseId) {
        return adoptService.getAdopt(caseId);
    }
}
