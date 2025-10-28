package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.CaseMode;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 50)
    private CaseMode mode; // "SOLO", "PARTY" 등 저장

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at") // 종료일 (null 허용)
    private LocalDateTime closedAt;

    @Builder
    public Case(CaseMode mode, String title, String status, CaseStatus status) {
        this.mode = mode;
        this.title = title;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // --- 비즈니스 로직 편의를 위한 메서드들 ---
    public void updateStatus(CaseStatus newStatus) {
        this.status = newStatus;
        if (CaseStatus.DONE.equals(newStatus)) {
            this.closedAt = LocalDateTime.now();
        }
    }
}