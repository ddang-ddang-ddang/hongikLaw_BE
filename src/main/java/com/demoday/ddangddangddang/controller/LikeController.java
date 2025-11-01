package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.like.LikeRequestDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 좋아요 토글 API
     * @param user (인증된 사용자 정보)
     * @param requestDto (contentId, contentType)
     * @return ApiResponse (성공 여부 및 상태 메시지)
     */
    @PostMapping
    public ApiResponse<Boolean> toggleLike(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody LikeRequestDto requestDto
    ) {
        // 1. 인증된 사용자 ID 가져오기
        Long userId = user.getUser().getId();

        // 2. 서비스 호출
        boolean isLiked = likeService.toggleLike(
                userId,
                requestDto.getContentId(),
                requestDto.getContentType()
        );

        String message = isLiked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.";
        return ApiResponse.onSuccess(message, isLiked);
    }
}

