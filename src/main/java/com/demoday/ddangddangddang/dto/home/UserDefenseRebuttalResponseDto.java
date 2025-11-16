package com.demoday.ddangddangddang.dto.home;

import com.demoday.ddangddangddang.domain.enums.CaseResult;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
        private CaseResult caseResult;
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
        private CaseResult caseResult;
    }

    private List<DefenseDto> defenses;
    private List<RebuttalDto> rebuttals;
}
