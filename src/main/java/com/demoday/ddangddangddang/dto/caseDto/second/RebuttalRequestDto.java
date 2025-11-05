package com.demoday.ddangddangddang.dto.caseDto.second;

import com.demoday.ddangddangddang.domain.enums.DebateSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RebuttalRequestDto {
    @NotNull(message = "변론 ID는 필수입니다.")
    private Long defenseId; // 이 반론이 달릴 원본 '변론' ID

    @NotNull(message = "입장을 선택해주세요.")
    private DebateSide type; // A 또는 B

    @NotBlank(message = "반론 내용을 입력해주세요.")
    private String content;

    // 부모 반론 ID (대대댓글 기능. 1단계 댓글은 null)
    private Long parentId;
}