package com.demoday.ddangddangddang.dto.report;

import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.domain.enums.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequestDto {

    @NotNull(message = "신고할 콘텐츠 ID는 필수입니다.")
    @Schema(description = "변론(Defense) 또는 반론(Rebuttal)의 ID", example = "1")
    private Long contentId;

    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    @Schema(description = "콘텐츠 타입 (DEFENSE, REBUTTAL)", example = "DEFENSE")
    private ContentType contentType;

    @NotNull(message = "신고 사유를 선택해주세요.")
    @Schema(description = "신고 사유 (PROFANITY, SLANDER, SPAM, ADVERTISEMENT, OBSCENE, OTHER)", example = "PROFANITY")
    private ReportReason reason;

    @Schema(description = "기타 사유 상세 내용 (선택)", example = "지나친 욕설이 포함되어 있습니다.")
    private String customReason;
}