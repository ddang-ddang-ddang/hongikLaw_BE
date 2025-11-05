package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.DebateSide;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private DebateSide type; // 'A' 또는 'B'

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt; // ✅ DB에 존재하는 컬럼 추가

    @Builder
    public Vote(Case aCase, User user, DebateSide type) {
        this.aCase = aCase;
        this.user = user;
        this.type = type;
    }

    @PrePersist
    protected void onCreate() {
        if (this.votedAt == null) {
            this.votedAt = LocalDateTime.now(); // ✅ 자동으로 현재 시각 삽입
        }
    }

    public void updateChoice(DebateSide newChoice) {
        this.type = newChoice;
        this.votedAt = LocalDateTime.now(); // ✅ 재투표 시 시간 갱신
    }
}
