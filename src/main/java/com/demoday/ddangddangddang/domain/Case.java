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
    private CaseMode mode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "aCase", cascade = CascadeType.ALL)
    private List<CaseParticipation> participations = new ArrayList<>();

    @Column(name = "appeal_deadline")
    private LocalDateTime appealDeadline;

    // [추가] 광고 여부 플래그
    @Column(name = "is_ad")
    private Boolean isAd = false;

    // [추가] 확장성: 클릭 시 이동할 외부 링크 (쇼핑몰, OTT 등)
    @Column(name = "ad_link")
    private String adLink;

    // [추가] 이미지 매칭: 광고 전용 커스텀 이미지 URL (배너 또는 배경)
    @Column(name = "ad_image_url")
    private String adImageUrl;

    @Builder
    public Case(CaseMode mode, String title, CaseStatus status) {
        this.mode = mode;
        this.title = title;
        this.status = status;
    }

    public void updateStatus(CaseStatus newStatus) {
        this.status = newStatus;
        if (CaseStatus.DONE.equals(newStatus)) {
            this.closedAt = LocalDateTime.now();
        }
    }

    // --- [ startAppeal 메서드 수정: deadline null 체크 추가 ] ---
    public void startAppeal(LocalDateTime deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("2차 재판 마감 기한(deadline)은 필수입니다.");
        }
        this.status = CaseStatus.SECOND;
        this.appealDeadline = deadline;
    }

    public void setThird() {
        this.status = CaseStatus.THIRD;
    }

    @PrePersist
    public void setDefaultAppealDeadline() {
        if (this.appealDeadline == null) {
            this.appealDeadline = LocalDateTime.now().plusHours(24);
        }
    }

    public void markAsAd(String adLink, String adImageUrl) {
        this.isAd = true;
        this.adLink = adLink;
        this.adImageUrl = adImageUrl;
    }
}