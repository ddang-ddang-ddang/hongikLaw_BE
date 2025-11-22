package com.demoday.ddangddangddang.controller.mainpage;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.home.CaseOnResponseDto;
import com.demoday.ddangddangddang.dto.home.CaseSimpleDto;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.mainpage.MainpageService;
import com.demoday.ddangddangddang.service.ranking.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequiredArgsConstructor
@RequestMapping("/api/home")
@Tag(name = "Mainpage API", description = "메인페이지(홈) 관련 API - by 황신애")
public class MainpageController {
    private final MainpageService mainpageService;
    private final RankingService rankingService;

    private final static int topN = 15;

    @Operation(summary = "유저의 진행중인 재판 조회", description = "로그인한 유저가 참여하고 있는 진행중인 재판 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "진행중인 재판 조회 성공",
                    content = @Content(mediaType = "application/json",
                            // ApiResponse<List<CaseOnResponseDto>> 스키마를 표현합니다.
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value =
                                    // 제공해주신 JSON 응답 예시를 여기에 붙여넣습니다.
                                    // (가독성을 위해 String으로 처리)
                                    "{\n" +
                                            "  \"isSuccess\": true,\n" +
                                            "  \"code\": \"COMMON2000\",\n" +
                                            "  \"message\": \"진행중인 재판 조회 성공\",\n" +
                                            "  \"result\": [\n" +
                                            "    {\n" +
                                            "      \"caseId\": 1,\n" +
                                            "      \"title\": \"환경 오염, 기업의 책임인가 개인의 책임인가?\",\n" +
                                            "      \"status\": \"SECOND\",\n" +
                                            "      \"mainArguments\": [\n" +
                                            "        \"기업의 무분별한 개발이 주 원인이다\",\n" +
                                            "        \"개인의 소비 패턴 변화가 시급하다\"\n" +
                                            "      ]\n" +
                                            "    },\n" +
                                            "    {\n" +
                                            "      \"caseId\": 2,\n" +
                                            "      \"title\": \"인공지능(AI) 창작물, 저작권을 인정해야 하는가?\",\n" +
                                            "      \"status\": \"FIRST\",\n" +
                                            "      \"mainArguments\": [\n" +
                                            "        \"창작의 주체는 AI 개발자에게 있다\",\n" +
                                            "        \"AI 스스로 학습한 결과이므로 독자적 권리가 필요하다\"\n" +
                                            "      ]\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"error\": null\n" +
                                            "}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @SecurityRequirement(name = "JWT TOKEN")
    @GetMapping("/users/cases")
    public ApiResponse<List<CaseOnResponseDto>> getCaseList(@AuthenticationPrincipal UserDetailsImpl user){
        Long userId = user.getUser().getId();
        return mainpageService.getCaseList(userId);
    }

    @Operation(summary = "유저의 변론 및 반론 조회", description = "로그인한 유저가 작성한 변론 및 반론 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 변론 및 반론 조회 완료",
                    content = @Content(mediaType = "application/json",
                            // ApiResponse<UserDefenseRebuttalResponseDto> 스키마
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value =
                                    // 제공해주신 JSON 응답 예시를 여기에 붙여넣습니다.
                                    "{\n" +
                                            "  \"isSuccess\": true,\n" +
                                            "  \"code\": \"COMMON2000\",\n" +
                                            "  \"message\": \"유저 변론 및 반론 조회 완료\",\n" +
                                            "  \"result\": {\n" +
                                            "    \"defenses\": [\n" +
                                            "      {\n" +
                                            "        \"caseId\": 1,\n" +
                                            "        \"defenseId\": 101,\n" +
                                            "        \"debateSide\": \"A\",\n" +
                                            "        \"title\": \"기업의 법적 책임을 강화하는 변론\",\n" +
                                            "        \"content\": \"기업은 이윤 추구 과정에서 발생하는 환경 파괴에 대해 무한한 책임을 져야 합니다...\",\n" +
                                            "        \"likeCount\": 15\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"rebuttals\": [\n" +
                                            "      {\n" +
                                            "        \"caseId\": 1,\n" +
                                            "        \"rebuttalId\": 201,\n" +
                                            "        \"debateSide\": \"B\",\n" +
                                            "        \"content\": \"기업의 책임도 중요하지만, 최종 소비자인 개인의 선택이 시장을 변화시킵니다...\",\n" +
                                            "        \"likeCount\": 5\n" +
                                            "      }\n" +
                                            "    ]\n" +
                                            "  },\n" +
                                            "  \"error\": null\n" +
                                            "}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @SecurityRequirement(name = "JWT TOKEN")
    @GetMapping("/users/defenses")
    public ApiResponse<UserDefenseRebuttalResponseDto> getDefenseList(@AuthenticationPrincipal UserDetailsImpl user){
        Long userId = user.getUser().getId();
        return mainpageService.getDefenseAndRebuttal(userId);
    }

    @GetMapping("/hot")
    public ApiResponse<List<CaseSimpleDto>> getHotCaseList(){
        return rankingService.getHotCases(topN);
    }
}
