package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.domain.enums.ReportReason;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reporter_id", "content_id", "content_type"}) // 중복 신고 방지
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter; // 신고자

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType; // DEFENSE(변론) or REBUTTAL(반론)

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReportReason reason; // 신고 사유

    @Column(name = "custom_reason", columnDefinition = "TEXT")
    private String customReason; // 기타 사유일 경우 상세 내용

    @Builder
    public Report(User reporter, Long contentId, ContentType contentType, ReportReason reason, String customReason) {
        this.reporter = reporter;
        this.contentId = contentId;
        this.contentType = contentType;
        this.reason = reason;
        this.customReason = customReason;
    }
}