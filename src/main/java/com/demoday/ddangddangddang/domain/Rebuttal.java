package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rebuttals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rebuttal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rebuttal_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defense_id", nullable = false)
    private Defense defense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private DebateSide type; // 'A' 또는 'B'

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Rebuttal(Defense defense, User user, DebateSide type, String content) {
        this.defense = defense;
        this.user = user;
        this.type = type;
        this.content = content;
        this.likesCount = 0;
        this.createdAt = LocalDateTime.now();
    }
}