package com.demoday.ddangddangddang.controller.mainpage;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.home.CaseOnResponseDto;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.service.mainpage.MainpageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class MainpageController {
    private final MainpageService mainpageService;

    @GetMapping("/users/cases")
    public ApiResponse<List<CaseOnResponseDto>> getCaseList(@AuthenticationPrincipal User user){
        Long userId = user.getId();
        return mainpageService.getCaseList(userId);
    }

    @GetMapping("/users/defenses")
    public ApiResponse<UserDefenseRebuttalResponseDto> getDefenseList(@AuthenticationPrincipal User user){
        Long userId = user.getId();
        return mainpageService.getDefensAndRebuttal(userId);
    }
}
