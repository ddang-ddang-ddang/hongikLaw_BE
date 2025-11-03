package com.demoday.ddangddangddang.controller.mypage;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.mypage.RankResponseDto;
import com.demoday.ddangddangddang.dto.mypage.RecordResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserAchievementResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserArchiveResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.service.mypage.MypageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "JWT TOKEN")
public class MypageController {
    private final MypageService mypageService;

    //로그인 및 스프링 세큐리티 구현 완료 후 파라미터 변경 예정 -완료-
    @Operation(summary = "유저 등급(랭크) 조회", description = "로그인한 유저의 현재 등급, 경험치, 랭크 이름을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 등급 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value =
                                    "{\n" +
                                            "  \"isSuccess\": true,\n" +
                                            "  \"code\": \"COMMON2000\",\n" +
                                            "  \"message\": \"\",\n" +
                                            "  \"result\": {\n" +
                                            "    \"id\" : 1,\n" +
                                            "    \"rank\" : \"변호사 1단계\",\n" +
                                            "    \"exp\" : 18363\n" +
                                            "  },\n" +
                                            "  \"error\": null\n" +
                                            "}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/rank")
    public ApiResponse<RankResponseDto> getRank(@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getRank(userId);
    }

    @Operation(summary = "유저 전적 조회", description = "로그인한 유저의 승리 및 패배 횟수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전적 조회에 성공했습니다",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value =
                                    "{\n" +
                                            "  \"isSuccess\": true,\n" +
                                            "  \"code\": \"COMMON2000\",\n" +
                                            "  \"message\": \"전적 조회에 성공했습니다\",\n" +
                                            "  \"result\": {\n" +
                                            "    \"id\" : 1,\n" +
                                            "    \"winCnt\" : 5,\n" +
                                            "    \"loseCnt\" : 3\n" +
                                            "  },\n" +
                                            "  \"error\": null\n" +
                                            "}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/record")
    public ApiResponse<RecordResponseDto> getRecord(@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getRecord(userId);
    }

    @Operation(summary = "유저 참여 사건 기록 조회", description = "로그인한 유저가 참여했던/참여중인 모든 사건 목록을 조회합니다. (진행중, 완료 포함)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 사건 리스트 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value =
                                    "{\n" +
                                            "  \"isSuccess\": true,\n" +
                                            "  \"code\": \"COMMON2000\",\n" +
                                            "  \"message\": \"유저 사건 리스트 조회 성공\",\n" +
                                            "  \"result\": [\n" +
                                            "    {\n" +
                                            "      \"caseId\": 101,\n" +
                                            "      \"title\": \"탕수육은 부먹인가 찍먹인가\",\n" +
                                            "      \"status\": \"DONE\",\n" +
                                            "      \"caseResult\": \"WIN\",\n" +
                                            "      \"mainArguments\": [\n" +
                                            "        \"바삭함이 생명이다\",\n" +
                                            "        \"원래 탕수육은 소스를 부어먹는 음식\"\n" +
                                            "      ]\n" +
                                            "    },\n" +
                                            "    {\n" +
                                            "      \"caseId\": 105,\n" +
                                            "      \"title\": \"여름휴가는 산 vs 바다\",\n" +
                                            "      \"status\": \"SECOND\",\n" +
                                            "      \"caseResult\": \"PENDING\",\n" +
                                            "      \"mainArguments\": [\n" +
                                            "        \"시원한 바다가 최고\",\n" +
                                            "        \"바다는 짠물이라 싫다\"\n" +
                                            "      ]\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"error\": null\n" +
                                            "}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/cases")
    public ApiResponse<List<UserArchiveResponseDto>> getUserCases (@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getUserCases(userId);
    }

    @Operation(summary = "유저 업적 조회", description = "로그인한 유저가 획득한 모든 업적 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 업적 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value =
                                    // 제공해주신 JSON 예시
                                    "{\n" +
                                            "  \"isSuccess\": true,\n" +
                                            "  \"code\": \"COMMON2000\",\n" +
                                            "  \"message\": \"유저 업적 조회 성공\",\n" +
                                            "  \"result\": [\n" +
                                            "    {\n" +
                                            "      \"userId\": 1,\n" +
                                            "      \"achievementId\": 101,\n" +
                                            "      \"achievementName\": \"첫 승리\",\n" +
                                            "      \"achievementDescription\": \"첫 번째 재판에서 승리했습니다.\",\n" +
                                            "      \"achievementIconUrl\": \"https://example.com/icons/first_win.png\",\n" +
                                            "      \"achievementTime\": \"2025-10-28T14:30:00\"\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"error\": null\n" +
                                            "}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저를 찾을 수 없습니다", content = @Content)
    })
    @GetMapping("/achievements")
    public ApiResponse<List<UserAchievementResponseDto>> getUserAchievement (@AuthenticationPrincipal(expression = "user") User user) {
        Long userId = user.getId();
        return mypageService.getAchievement(userId);
    }
}
