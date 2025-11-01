package com.demoday.ddangddangddang.dto.home;

import com.demoday.ddangddangddang.domain.enums.DebateSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@Builder
public class UserDefenseRebuttalResponseDto {

    @Getter
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class DefenseDto{
        private Long caseId;
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
    public static class RebuttalDto{
        private Long caseId;
        private Long defenseId;
        private Long rebuttalId;
        private DebateSide debateSide;
        private String content;
        private int likeCount;
    }

    private List<DefenseDto> defenses;
    private List<RebuttalDto> rebuttals;
}
