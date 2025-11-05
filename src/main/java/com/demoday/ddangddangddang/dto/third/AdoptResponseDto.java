package com.demoday.ddangddangddang.dto.third;

import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@Builder
public class AdoptResponseDto {
    @Getter
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class DefenseAdoptDto{
        private Long caseId;
        private Long userId;
        private Long defenseId;
        private DebateSide debateSide;
        private String title;
        private String content;
        private int likeCount;
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class RebuttalAdoptDto{
        private Long caseId;
        private Long userId;
        private Long defenseId;
        private Long rebuttalId;
        private Long parentId;
        private String parentContent;
        private DebateSide debateSide;
        private String content;
        private int likeCount;
    }

    private List<DefenseAdoptDto> defenses;
    private List<RebuttalAdoptDto> rebuttals;
}
