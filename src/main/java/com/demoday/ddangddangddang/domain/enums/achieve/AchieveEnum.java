package com.demoday.ddangddangddang.domain.enums.achieve;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AchieveEnum implements AchieveEnumBase {

    FIRST_CASE("첫 사건", "첫 사건을 생성했을 때 얻습니다.", "first_case.png"),
    FIRST_VS("첫 VS 모드 참여", "첫 vs 모드에 참여했을 때 얻습니다.", "first_vs.png"),
    FIRST_DEFENSE("첫 변론", "처음으로 변론에 참여했을 때 얻습니다.", "first_defense.png"),
    FIRST_REBUTTAL("첫 반론", "처음으로 반론에 참여했을 때 얻습니다.", "first_rebuttal.png"),
    FIRST_WIN("첫 승리", "처음으로 재판에 승리했을 때 얻습니다.", "first_win.png"),
    FIRST_ADOPT("첫 채택", "처음으로 변론/반론이 채택된 경우 얻습니다.", "first_adopt.png"),
    LIKE_50("좋아요 50개 달성", "좋아요 50개를 받았습니다", "like_50.png"),
    LIKE_100("좋아요 100개 달성", "좋아요 100개를 받았습니다.", "like_100.png"),
    CASE_10("재판 10개 생성", "재판을 10개 생성했습니다.", "case_10.png"),
    DEFENSE_10("변론 10개 생성", "변론을 10개 생성했습니다.", "defense_10.png"),
    DEFENSE_50("변론 50개 생성", "변론을 50개 생성했습니다.", "defense_50.png"),
    REBUTTAL_50("반론 50개 생성", "반론을 50개 생성했습니다.", "rebuttal_50.png"),
    WIN_10("10승","재판 승리 10회","win_10.png")
    ;

    private final String name;
    private final String description;
    private final String fileName; // 파일명만 관리

    private static final String BASE_URL = "https://ddangddangddang-demoday.s3.ap-northeast-2.amazonaws.com/icons/";

    public String getIconUrl() {
        return BASE_URL + this.fileName;
    }
}
