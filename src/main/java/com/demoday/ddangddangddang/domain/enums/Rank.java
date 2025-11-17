package com.demoday.ddangddangddang.domain.enums;

public enum Rank {
    PARTNER_LAWYER("파트너 변호사", 4000L),
    SENIOR_LAWYER("시니어 변호사", 3000L),
    MID_LEVEL_LAWYER("중견 변호사", 2200L),
    JUNIOR_LAWYER("신입 변호사", 1600L),
    LAW_SCHOOL_STUDENT("로스쿨생", 1100L),
    LAW_STUDENT("법대생", 700L),
    MASTER_BRAWLER("말싸움 고수", 400L),
    MID_BRAWLER("말싸움 중수", 200L),
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
