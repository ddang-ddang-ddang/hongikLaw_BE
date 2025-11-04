package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.dto.caseDto.CaseDetailResponseDto;
import com.demoday.ddangddangddang.dto.caseDto.CaseRequestDto;
import com.demoday.ddangddangddang.dto.caseDto.CaseResponseDto;
import com.demoday.ddangddangddang.dto.caseDto.CaseStatusRequestDto;
import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Case API", description = "사건(1차, 2차 재판) 관련 API")
@SecurityRequirement(name = "JWT TOKEN") // 이 컨트롤러의 모든 API는 인증 필요
@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @Operation(summary = "1차 재판(초심) 생성 (솔로 모드)", description = "주제, A/B 입장과 근거를 받아 새 사건을 생성하고 AI 판결을 요청합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "사건 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"사건이 성공적으로 생성되었습니다.\",\"result\":{\"caseId\":1},\"error\":null}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"REQ_4002\",\"message\":\"파라미터 형식이 잘못되었습니다.\",\"result\":null,\"error\":\"[title] 사건 설명을 입력해주세요.\"}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"AUTH_4011\",\"message\":\"인증 정보가 누락되었습니다.\",\"result\":null,\"error\":\"Full authentication is required to access this resource\"}")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CaseResponseDto>> createCase(
            @Valid @RequestBody CaseRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseResponseDto responseDto = caseService.createCase(requestDto, userDetails.getUser());

        ApiResponse<CaseResponseDto> response = ApiResponse.onSuccess(
                "사건이 성공적으로 생성되었습니다.",
                responseDto
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "사건 상세 조회 (1차 입장문 포함)", description = "특정 사건의 상세 내용(주제, 상태)과 1차 입장문(A/B)을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사건 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\n  \"isSuccess\": true,\n  \"code\": \"COMMON2000\",\n  \"message\": \"사건 상세 조회에 성공하였습니다.\",\n  \"result\": {\n    \"caseId\": 1,\n    \"title\": \"깻잎 논쟁\",\n    \"status\": \"FIRST\",\n    \"mode\": \"SOLO\",\n    \"argumentA\": {\n      \"mainArgument\": \"바삭함이 생명이다\",\n      \"reasoning\": \"소스가 부어지면...\",\n      \"authorId\": 1\n    },\n    \"argumentB\": {\n      \"mainArgument\": \"원래 부어먹는 음식\",\n      \"reasoning\": \"소스가 스며들어야...\",\n      \"authorId\": 1\n    }\n  },\n  \"error\": null\n}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "조회 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"AUTH_4031\",\"message\":\"접근 권한이 없습니다.\",\"result\":null,\"error\":\"해당 사건에 대한 조회 권한이 없습니다.\"}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 사건을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"INVALID_PARAMETER\",\"message\":\"파라미터 형식이 잘못되었습니다.\",\"result\":null,\"error\":\"해당 사건을 찾을 수 없습니다.\"}")
                    )
            )
    })
    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CaseDetailResponseDto>> getCaseDetail(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseDetailResponseDto responseDto = caseService.getCaseDetail(caseId, userDetails.getUser());
        ApiResponse<CaseDetailResponseDto> response = ApiResponse.onSuccess(
                "사건 상세 조회에 성공하였습니다.",
                responseDto
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "1차 재판(초심) AI 판결 결과 조회", description = "AI가 내린 1차 판결문(내용, 결론, 비율)을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "1차 판결 결과 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\n  \"isSuccess\": true,\n  \"code\": \"COMMON2000\",\n  \"message\": \"1차 판결 결과 조회에 성공하였습니다.\",\n  \"result\": {\n    \"judgeIllustrationUrl\": \"https://example.com/images/judge.png\",\n    \"verdict\": \"A측의 주장이 더 설득력 있습니다...\",\n    \"conclusion\": \"A측의 손을 들어줍니다.\",\n    \"ratioA\": 70,\n    \"ratioB\": 30\n  },\n  \"error\": null\n}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사건을 찾을 수 없거나 아직 판결이 완료되지 않음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":false,\"code\":\"INVALID_PARAMETER\",\"message\":\"파라미터 형식이 잘못되었습니다.\",\"result\":null,\"error\":\"아직 판결이 완료되지 않았습니다.\"}")
                    )
            )
    })
    @GetMapping("/{caseId}/judgment")
    public ResponseEntity<ApiResponse<JudgmentResponseDto>> getJudgment(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        JudgmentResponseDto responseDto = caseService.getJudgment(caseId, userDetails.getUser());

        ApiResponse<JudgmentResponseDto> response = ApiResponse.onSuccess(
                "1차 판결 결과 조회에 성공하였습니다.",
                responseDto
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "1차 재판 상태 변경 (2차 진행/종료)", description = "1차 판결 확인 후, 2차 재판으로 넘기거나(SECOND) 사건을 완전히 종료(DONE)시킵니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"사건 상태가 성공적으로 변경되었습니다.\",\"result\":null,\"error\":null}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값 요청 (예: `FIRST`를 요청)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 사건을 찾을 수 없습니다.", content = @Content)
    })
    @PatchMapping("/{caseId}/status")
    public ResponseEntity<ApiResponse<Void>> updateCaseStatus(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseStatusRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        caseService.updateCaseStatus(caseId, requestDto, userDetails.getUser());

        ApiResponse<Void> response = ApiResponse.onSuccess(
                "사건 상태가 성공적으로 변경되었습니다."
        );
        return ResponseEntity.ok(response);
    }
}