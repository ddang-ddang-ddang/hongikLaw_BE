package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.like.LikeRequestDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Tag(name = "Like API", description = "좋아요 관련 API - by 황신애")
public class LikeController {

    private final LikeService likeService;

    /**
     * 좋아요 토글 API
     * @param user (인증된 사용자 정보)
     * @param requestDto (contentId, contentType)
     * @return ApiResponse (성공 여부 및 상태 메시지)
     */
    @Operation(summary = "좋아요 토글", description = "특정 콘텐츠에 대해 좋아요를 누르거나 취소합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "좋아요 추가", value =
                                            "{\"status\": 200, \"message\": \"좋아요가 추가되었습니다.\", \"data\": true}"),
                                    @ExampleObject(name = "좋아요 취소", value =
                                            "{\"status\": 200, \"message\": \"좋아요가 취소되었습니다.\", \"data\": false}")
                            })
            )
    })
    @SecurityRequirement(name = "JWT TOKEN")
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

