package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creatorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_user_id") // 상대방 User FK (nullable)
    private User opponentUser;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at") // 종료일 (null 허용)
    private LocalDateTime closedAt;

    @Builder
    public Case(User creatorUser, User opponentUser, String title, String status) {
        this.creatorUser = creatorUser;
        this.opponentUser = opponentUser; // null 가능
        this.title = title;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // --- 비즈니스 로직 편의를 위한 메서드들 ---
    public void updateStatus(String newStatus) {
        this.status = newStatus;
        if ("종료".equals(newStatus)) {
            this.closedAt = LocalDateTime.now();
        }
    }
}