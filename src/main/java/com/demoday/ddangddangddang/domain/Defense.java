package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "defenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Defense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defense_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // 찬성/반대 (변론의 입장)

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount; // 추천 수

    @Column(name = "is_adopted", nullable = false)
    private Boolean isAdopted; // 최종심 채택 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Defense(Case aCase, User user, String type, String content) {
        this.aCase = aCase;
        this.user = user;
        this.type = type;
        this.content = content;
        this.likesCount = 0;
        this.isAdopted = false;
        this.createdAt = LocalDateTime.now();
    }

    // --- 비즈니스 로직 편의를 위한 메서드들 ---
    public void addLike() {
        this.likesCount++;
    }

    public void markAsAdopted() {
        this.isAdopted = true;
    }
}