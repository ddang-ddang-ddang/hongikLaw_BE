package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ranks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rank_id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name; // 등급명 (예: 법대생, 로스쿨 수석)

    @Column(name = "required_exp", nullable = false)
    private Long requiredExp; // 해당 등급 도달에 필요한 경험치

    @Builder
    public Rank(String name, Long requiredExp) {
        this.name = name;
        this.requiredExp = requiredExp;
    }
}