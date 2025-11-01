package com.demoday.ddangddangddang.controller.mypage;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.mypage.RankResponseDto;
import com.demoday.ddangddangddang.dto.mypage.RecordResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserAchievementResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserArchiveResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.service.mypage.MypageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Mypage", description = "마이페이지 API -by 황신애")
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class MypageController {
    private final MypageService mypageService;

    //로그인 및 스프링 세큐리티 구현 완료 후 파라미터 변경 예정 -완료-
    @GetMapping("/rank")
    public ApiResponse<RankResponseDto> getRank(@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getRank(userId);
    }

    @GetMapping("/record")
    public ApiResponse<RecordResponseDto> getRecord(@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getRecord(userId);
    }

    @GetMapping("/cases")
    public ApiResponse<List<UserArchiveResponseDto>> getUserCases (@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getUserCases(userId);
    }

    @GetMapping("/achievements")
    public ApiResponse<List<UserAchievementResponseDto>> getUserAchievement (@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getAchievement(userId);
    }
}
