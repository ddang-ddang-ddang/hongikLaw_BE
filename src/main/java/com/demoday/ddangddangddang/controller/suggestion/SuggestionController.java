package com.demoday.ddangddangddang.controller.suggestion;

import com.demoday.ddangddangddang.dto.suggestion.SuggestionRequestDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.suggestion.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
@Tag(name = "Suggestion API", description = "건의사항 API")
@SecurityRequirement(name = "JWT TOKEN")
public class SuggestionController {

    private final SuggestionService suggestionService;

    @Operation(summary = "건의사항 등록", description = "서비스 이용에 대한 건의사항이나 피드백을 보냅니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createSuggestion(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SuggestionRequestDto requestDto
    ) {
        suggestionService.createSuggestion(userDetails.getUser().getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("소중한 의견 감사합니다. 건의사항이 접수되었습니다."));
    }
}