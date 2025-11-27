package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity { // BaseEntity 상속 (createdAt 자동 생성)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User receiver; // 알림 받는 사람

    @Column(nullable = false)
    private String message;

    private String iconUrl;

    private Long caseId;
    private Long defenseId;
    private Long rebuttalId;
    private Long judgementId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false; // 읽음 여부

    public void markAsRead() {
        this.isRead = true;
    }
}
