package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

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

    @Column(name = "choice", nullable = false, length = 50)
    private String choice; // 찬성/반대

    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;

    @Builder
    public Vote(Case aCase, User user, String choice) {
        this.aCase = aCase;
        this.user = user;
        this.choice = choice;
        this.votedAt = LocalDateTime.now();
    }
}