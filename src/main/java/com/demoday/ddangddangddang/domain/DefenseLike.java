package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "defense_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "defense_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DefenseLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defense_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defense_id", nullable = false)
    private Defense defense;

    public static DefenseLike of(User user, Defense defense) {
        return DefenseLike.builder()
                .user(user)
                .defense(defense)
                .build();
    }
}
