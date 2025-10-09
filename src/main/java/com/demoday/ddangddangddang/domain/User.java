package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    private Rank rank;

    @Column(name = "nickname", nullable = false, unique = true, length = 255)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "exp", nullable = false)
    private Long exp;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "win_cnt")
    private Integer winCnt = 0;

    @Column(name = "lose_cnt")
    private Integer loseCnt = 0;

    @Builder
    public User(Rank rank, String nickname, String email, String password, Long exp, Integer totalPoints, Integer winCnt, Integer loseCnt) {
        this.rank = rank;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.exp = exp;
        this.totalPoints = totalPoints;
        this.winCnt = winCnt;
        this.loseCnt = loseCnt;
    }

    // --- 비즈니스 로직 편의를 위한 메서드들 ---
    public void addExp(Long amount) {
        this.exp += amount;
        // 경험치 증가 후 랭크업 로직이 있다면 여기서 호출
    }

    public void addPoints(Integer amount) {
        this.totalPoints += amount;
    }

    public void deductPoints(Integer amount) {
        if (this.totalPoints < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.totalPoints -= amount;
    }
}