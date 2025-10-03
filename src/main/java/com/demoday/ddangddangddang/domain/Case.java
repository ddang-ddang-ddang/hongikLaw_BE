package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id", nullable = false)
    private Long id;

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
    public Case(String title, CaseStatus status) {
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