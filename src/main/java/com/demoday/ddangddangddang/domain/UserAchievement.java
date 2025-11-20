package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.achieve.AchieveEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_achievements", uniqueConstraints = { // user_id와 achievement_id 조합의 유니크 제약 추가
        @UniqueConstraint(columnNames = {"user_id", "achievement_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_achievement_id", nullable = false)
    private Long id; // 단일 PK로 사용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement", nullable = false)
    private AchieveEnum achievement;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    @Builder
    public UserAchievement(User user, AchieveEnum achievement) {
        this.user = user;
        this.achievement = achievement;
        this.earnedAt = LocalDateTime.now();
    }
}