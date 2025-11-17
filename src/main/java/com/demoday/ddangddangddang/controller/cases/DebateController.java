package com.demoday.ddangddangddang.controller.cases;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.dto.caseDto.second.*;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.cases.DebateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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

@Tag(name = "Debate (2nd Trial) API", description = "2차 재판(토론, 투표) 관련 API - by 최우혁")
@SecurityRequirement(name = "JWT TOKEN")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DebateController {

    private final DebateService debateService;

    @Operation(summary = "2차 재판 시작", description = "1차 판결 후, 2차 재판(SECOND)을 시작합니다. (시간제한 없음)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "2차 재판 시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 2차 재판이 시작된 경우", content = @Content)
    })
    @PatchMapping("/cases/{caseId}/appeal")
    public ResponseEntity<ApiResponse<Void>> startAppeal(
            @PathVariable Long caseId,
            @Valid @RequestBody AppealRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        debateService.startAppeal(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("2차 재판이 성공적으로 시작되었습니다."));
    }

    @Operation(summary = "2차 재판 상세 정보 조회", description = "2차 재판에 필요한 모든 정보(주제, 변론, 중첩 반론, 내 투표 현황, 실시간 AI 판결)를 조회합니다.")
    @GetMapping("/cases/{caseId}/debate")
    public ResponseEntity<ApiResponse<CaseDetail2ndResponseDto>> getDebateDetails(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CaseDetail2ndResponseDto responseDto = debateService.getDebateDetails(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("2차 재판 정보 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "2차 재판 '변론' 목록 조회", description = "2차 재판의 모든 '변론' 목록과 각 변론의 '반론 개수'를 조회합니다.")
    @GetMapping("/cases/{caseId}/defenses")
    public ResponseEntity<ApiResponse<List<DefenseResponseDto>>> getDefensesByCase(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<DefenseResponseDto> responseDto = debateService.getDefensesByCase(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("변론 목록 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "2차 재판 '반론' 목록 조회 (중첩)", description = "특정 변론(defenseId)에 달린 모든 '반론' 목록을 중첩(대댓글) 구조로 조회합니다.")
    @GetMapping("/defenses/{defenseId}/rebuttals")
    public ResponseEntity<ApiResponse<List<RebuttalResponseDto>>> getRebuttalsByDefense(
            @PathVariable Long defenseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<RebuttalResponseDto> responseDto = debateService.getRebuttalsByDefense(defenseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("반론 목록 조회에 성공하였습니다.", responseDto));
    }

    @Operation(summary = "변론 제출", description = "2차 재판에 A측 또는 B측의 변론을 제출합니다. (제출 시 AI 판결이 비동기 업데이트됩니다.)")
    @PostMapping("/cases/{caseId}/defenses")
    public ResponseEntity<ApiResponse<Long>> createDefense(
            @PathVariable Long caseId,
            @Valid @RequestBody DefenseRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Defense defense = debateService.createDefense(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("변론이 성공적으로 등록되었습니다.", defense.getId()));
    }

    @Operation(summary = "반론(대댓글) 제출", description = "특정 변론(defenseId) 또는 반론(parentId)에 대해 A/B 입장을 선택하여 반론을 제출합니다.")
    @PostMapping("/rebuttals")
    public ResponseEntity<ApiResponse<Long>> createRebuttal(
            @Valid @RequestBody RebuttalRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Rebuttal rebuttal = debateService.createRebuttal(requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("반론이 성공적으로 등록되었습니다.", rebuttal.getId()));
    }

    @Operation(summary = "배심원 투표", description = "2차 재판에서 A측 또는 B측에 투표합니다. (재투표 시 입장 변경, 투표 시 AI 판결 비동기 업데이트)")
    @PostMapping("/cases/{caseId}/vote")
    public ResponseEntity<ApiResponse<Void>> castVote(
            @PathVariable Long caseId,
            @Valid @RequestBody VoteRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        debateService.castVote(caseId, requestDto, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("투표가 완료되었습니다."));
    }

    @Operation(summary = "2차 재판 투표 결과 조회", description = "현재 사건의 A/B측 투표 현황 및 퍼센트를 조회합니다.")
    @GetMapping("/cases/{caseId}/vote/result")
    public ResponseEntity<ApiResponse<VoteResultResponseDto>> getVoteResult(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        VoteResultResponseDto resultDto = debateService.getVoteResult(caseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.onSuccess("투표 결과 조회에 성공하였습니다.", resultDto));
    }
}