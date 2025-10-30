package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.CaseMode;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Case extends BaseEntity {

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

    @Column(name = "closed_at") // 종료일 (null 허용)
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "aCase", cascade = CascadeType.ALL)
    private List<CaseParticipation> participations = new ArrayList<>();

    @Builder
    public Case(CaseMode mode, String title, CaseStatus status) {
        this.mode = mode;
        this.title = title;
        this.status = status;
    }

    // --- 비즈니스 로직 편의를 위한 메서드들 ---
    public void updateStatus(CaseStatus newStatus) {
        this.status = newStatus;
        if (CaseStatus.DONE.equals(newStatus)) {
            this.closedAt = LocalDateTime.now();
        }
    }
}