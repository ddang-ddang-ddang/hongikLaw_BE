package com.demoday.ddangddangddang.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Rank {
    PARTNER_LAWYER("파트너 변호사", 8500L),
    SENIOR_LAWYER("시니어 변호사", 6500L),
    MID_LEVEL_LAWYER("중견 변호사", 5000L),
    JUNIOR_LAWYER("신입 변호사", 4100L),

    LAW_SCHOOL_GRADUATE("로스쿨 졸업반", 3650L),
    LAW_SCHOOL_2L("로스쿨 2학년", 3000L),
    LAW_SCHOOL_1L("로스쿨 1학년", 2400L),

    LAW_STUDENT_SENIOR("법대생 졸업반", 1900L),
    LAW_STUDENT_JUNIOR("법대생 3학년", 1400L),
    LAW_STUDENT_SOPHOMORE("법대생 2학년", 1000L),
    LAW_STUDENT_FRESHMAN("법대생 1학년", 700L),

    MASTER_BRAWLER("말싸움 고수", 500L),
    MID_BRAWLER("말싸움 중수", 250L),
    NOVICE_BRAWLER("말싸움 하수", 100L),
    NEWBIE_BRAWLER("말싸움 풋내기", 0L);

    @JsonValue
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
