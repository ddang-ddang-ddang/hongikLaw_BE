package com.demoday.ddangddangddang.dto.third;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgementDetailResponseDto {
    private Long judgmentId;
    private String content;
    private Integer ratioA;
    private Integer ratioB;
    private Boolean isAd;
    private String adLink;
    private String adImageUrl;

    // 이전에 만든 DTO 재활용
    private List<AdoptResponseDto.DefenseAdoptDto> adoptedDefenses;
    private List<AdoptResponseDto.RebuttalAdoptDto> adoptedRebuttals;
}
