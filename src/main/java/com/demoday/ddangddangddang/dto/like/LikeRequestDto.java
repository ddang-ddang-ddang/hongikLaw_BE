package com.demoday.ddangddangddang.dto.like;

import com.demoday.ddangddangddang.domain.enums.ContentType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LikeRequestDto {

    @NotNull(message = "콘텐츠 ID는 필수입니다.")
    private Long contentId;

    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    private ContentType contentType; // DEFENSE 또는 REBUTTAL

    @Builder
    public LikeRequestDto(Long contentId, ContentType contentType) {
        this.contentId = contentId;
        this.contentType = contentType;
    }
}
