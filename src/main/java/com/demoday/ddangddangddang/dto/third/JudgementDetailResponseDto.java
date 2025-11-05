package com.demoday.ddangddangddang.dto.third;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class JudgementDetailResponseDto {
    private Long judgmentId;
    private String content;
    private Integer ratioA;
    private Integer ratioB;
    private LocalDateTime createdAt; // 판결 생성일 (Judgment 엔티티에 있다고 가정)

    // 이전에 만든 DTO 재활용
    private List<AdoptResponseDto.DefenseAdoptDto> adoptedDefenses;
    private List<AdoptResponseDto.RebuttalAdoptDto> adoptedRebuttals;
}
