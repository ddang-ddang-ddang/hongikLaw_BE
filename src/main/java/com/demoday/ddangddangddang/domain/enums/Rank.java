package com.demoday.ddangddangddang.domain.enums;

import lombok.Getter;

@Getter
public enum Rank {
    PARTNER_LAWYER("파트너 변호사", 6000L),
    SENIOR_LAWYER("시니어 변호사", 5200L),
    MID_LEVEL_LAWYER("중견 변호사", 4500L),
    JUNIOR_LAWYER("신입 변호사", 3850L),

    LAW_SCHOOL_GRADUATE("로스쿨 졸업반", 3250L),
    LAW_SCHOOL_2L("로스쿨 2학년", 2700L),
    LAW_SCHOOL_1L("로스쿨 1학년", 2200L),

    LAW_STUDENT_SENIOR("법대생 졸업반", 1750L),
    LAW_STUDENT_JUNIOR("법대생 3학년", 1350L),
    LAW_STUDENT_SOPHOMORE("법대생 2학년", 1000L),
    LAW_STUDENT_FRESHMAN("법대생 1학년", 700L),

    MASTER_BRAWLER("말싸움 고수", 450L),
    MID_BRAWLER("말싸움 중수", 250L),
    NOVICE_BRAWLER("말싸움 하수", 100L),
    NEWBIE_BRAWLER("말싸움 풋내기", 0L);

    private final String displayName;
    private final Long minExp;

    Rank(String displayName, Long minExp) {
        this.displayName = displayName;
        this.minExp = minExp;
    }

    /**
     * EXP에 맞는 랭크를 반환합니다.
     * (minExp가 높은 순서대로 검사)
     */
    public static Rank getRankByExp(Long exp) {
        for (Rank rank : Rank.values()) {
            if (exp >= rank.getMinExp()) {
                return rank;
            }
        }
        return NEWBIE_BRAWLER; // 기본값
    }
}
