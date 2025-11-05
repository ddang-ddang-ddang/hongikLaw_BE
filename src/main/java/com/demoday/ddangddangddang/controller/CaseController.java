package com.demoday.ddangddangddang.controller;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.dto.caseDto.second.*;
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
import java.util.List;

@Tag(name = "Case API", description = "사건(1차, 2차 재판) 관련 API")
@SecurityRequirement(name = "JWT TOKEN")
@RestController
@RequestMapping("/api/v1") // [수정] 경로 충돌을 피하기 위해 /cases는 메서드 레벨로 이동
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    // --- [ 1차 재판 API (기존) ] ---

    @Operation(summary = "1차 재판(초심) 생성 (솔로 모드)")
    @ApiResponses(value = { /* ... (Swagger) ... */ })
    @PostMapping("/cases")
    public ResponseEntity<ApiResponse<CaseResponseDto>> createCase(
            @Valid @RequestBody CaseRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseResponseDto responseDto = caseService.createCase(requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("사건이 성공적으로 생성되었습니다.", responseDto));
    }

    @Operation(summary = "사건 상세 조회 (1차 입장문 포함)")
    @ApiResponses(value = { /* ... (Swagger) ... */ })
    @GetMapping("/cases/{caseId}")
    public ResponseEntity<ApiResponse<CaseDetailResponseDto>> getCaseDetail(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseDetailResponseDto responseDto = caseService.getCaseDetail(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("사건 상세 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "1차 재판(초심) AI 판결 결과 조회")
    @ApiResponses(value = { /* ... (Swagger) ... */ })
    @GetMapping("/cases/{caseId}/judgment")
    public ResponseEntity<ApiResponse<JudgmentResponseDto>> getJudgment(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        JudgmentResponseDto responseDto = caseService.getJudgment(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("1차 판결 결과 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "1차 재판 상태 변경 (종료)", description = "1차 판결 확인 후, 사건을 완전히 종료(DONE)시킵니다.")
    @ApiResponses(value = { /* ... (Swagger) ... */ })
    @PatchMapping("/cases/{caseId}/status")
    public ResponseEntity<ApiResponse<Void>> updateCaseStatus(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseStatusRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        caseService.updateCaseStatus(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("사건 상태가 성공적으로 변경되었습니다."));
    }

    // --- [ 2차 재판 API (신규) ] ---

    @Operation(summary = "2차 재판 시작", description = "1차 판결 후, 2차 재판(SECOND)을 시작합니다. (시간제한 없음)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "2차 재판 시작 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"2차 재판이 성공적으로 시작되었습니다.\",\"result\":null,\"error\":null}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 2차 재판이 시작된 경우", content = @Content)
    })
    @PatchMapping("/cases/{caseId}/appeal")
    public ResponseEntity<ApiResponse<Void>> startAppeal(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        caseService.startAppeal(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("2차 재판이 성공적으로 시작되었습니다."));
    }

    @Operation(summary = "2차 재판 상세 정보 조회", description = "2차 재판에 필요한 모든 정보(주제, 변론, 중첩 반론, 내 투표 현황, 실시간 AI 판결)를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "2차 재판 정보 조회 성공", content = @Content(schema = @Schema(implementation = CaseDetail2ndResponseDto.class)))
    })
    @GetMapping("/cases/{caseId}/second")
    public ResponseEntity<ApiResponse<CaseDetail2ndResponseDto>> getCaseDefenses(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseDetail2ndResponseDto responseDto = caseService.getCaseDefenses(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("2차 재판 정보 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "2차 재판 '변론' 목록 조회", description = "2차 재판의 모든 '변론' 목록과 각 변론의 '반론 개수'를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변론 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"변론 목록 조회에 성공하였습니다.\",\"result\":[{\"defenseId\":1,\"authorNickname\":\"판사1\",\"side\":\"A\",\"content\":\"A측 변론입니다.\",\"likesCount\":10,\"isLikedByMe\":true,\"rebuttalCount\":5}],\"error\":null}")
                    )
            )
    })
    @GetMapping("/cases/{caseId}/defenses")
    public ResponseEntity<ApiResponse<List<DefenseResponseDto>>> getDefensesByCase( // [수정] 반환 타입 변경
                                                                                    @PathVariable Long caseId,
                                                                                    @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<DefenseResponseDto> responseDto = caseService.getDefensesByCase(caseId, userDetails.getUser()); // [수정] 호출 메서드 변경
        return ResponseEntity.ok(ApiResponse.onSuccess("변론 목록 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "2차 재판 '반론' 목록 조회 (중첩)", description = "특정 변론(defenseId)에 달린 모든 '반론' 목록을 중첩(대댓글) 구조로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "반론 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"반론 목록 조회에 성공하였습니다.\",\"result\":[{\"rebuttalId\":10,\"parentId\":null,\"authorNickname\":\"유저1\",\"type\":\"A\",\"content\":\"지지합니다.\",\"likesCount\":3,\"isLikedByMe\":false,\"children\":[{\"rebuttalId\":15,\"parentId\":10,...}]}],\"error\":null}")
                    )
            )
    })
    @GetMapping("/defenses/{defenseId}/rebuttals") // [신규]
    public ResponseEntity<ApiResponse<List<RebuttalResponseDto>>> getRebuttalsByDefense(
            @PathVariable Long defenseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<RebuttalResponseDto> responseDto = caseService.getRebuttalsByDefense(defenseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("반론 목록 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "변론 제출", description = "2차 재판에 A측 또는 B측의 변론을 제출합니다. (제출 시 AI 판결이 비동기 업데이트됩니다.)")
    @ApiResponses(value = { @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "변론 등록 성공") })
    @PostMapping("/cases/{caseId}/defenses")
    public ResponseEntity<ApiResponse<Long>> createDefense(
            @PathVariable Long caseId,
            @Valid @RequestBody DefenseRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Defense defense = caseService.createDefense(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("변론이 성공적으로 등록되었습니다.", defense.getId()));
    }

    @Operation(summary = "반론(대댓글) 제출", description = "특정 변론(defenseId) 또는 반론(parentId)에 대해 A/B 입장을 선택하여 반론을 제출합니다.")
    @ApiResponses(value = { @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "반론 등록 성공") })
    @PostMapping("/rebuttals") // [수정] 컨트롤러의 복잡도를 낮추기 위해 별도 경로 사용
    public ResponseEntity<ApiResponse<Long>> createRebuttal(
            @Valid @RequestBody RebuttalRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Rebuttal rebuttal = caseService.createRebuttal(requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("반론이 성공적으로 등록되었습니다.", rebuttal.getId()));
    }

    @Operation(summary = "배심원 투표", description = "2차 재판에서 A측 또는 B측에 투표합니다. (재투표 시 입장 변경, 투표 시 AI 판결 비동기 업데이트)")
    @ApiResponses(value = { @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "투표 완료") })
    @PostMapping("/cases/{caseId}/vote")
    public ResponseEntity<ApiResponse<Void>> castVote(
            @PathVariable Long caseId,
            @Valid @RequestBody VoteRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        caseService.castVote(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("투표가 완료되었습니다."));
    }

    @Operation(summary = "2차 재판 투표 결과 조회", description = "현재 사건의 A/B측 투표 현황 및 퍼센트를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "투표 결과 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"isSuccess\":true,\"code\":\"COMMON2000\",\"message\":\"투표 결과 조회에 성공하였습니다.\",\"result\":{\"totalVotes\":25,\"aCount\":15,\"bCount\":10,\"aPercent\":60.0,\"bPercent\":40.0},\"error\":null}")
                    )
            )
    })
    @GetMapping("/cases/{caseId}/vote/result")
    public ResponseEntity<ApiResponse<VoteResultResponseDto>> getVoteResult(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        VoteResultResponseDto resultDto = caseService.getVoteResult(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("투표 결과 조회에 성공하였습니다.", resultDto));
    }
}