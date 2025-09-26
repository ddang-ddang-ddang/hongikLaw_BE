package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "achievements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "achievement_id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 칭호명

    @Column(name = "description", columnDefinition = "TEXT") // null 허용
    private String description; // 달성 조건이나 상세 설명

    @Column(name = "icon_url", length = 255) // null 허용
    private String iconUrl; // 칭호에 대한 이미지 URL

    @Builder
    public Achievement(String title, String description, String iconUrl) {
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
    }
}