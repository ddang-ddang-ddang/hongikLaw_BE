package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exp_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exp_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false)
    private Long amount; // 획득/차감량 (예: +100, -50)

    @Column(name = "description", nullable = false)
    private String description; // 변동 사유 (예: "사건 승리", "변론 작성")

    @Builder
    public ExpLog(User user, Long amount, String description) {
        this.user = user;
        this.amount = amount;
        this.description = description;
    }
}