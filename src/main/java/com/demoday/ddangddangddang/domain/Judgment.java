package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "judgments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Judgment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "judgment_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @Column(name = "stage", nullable = false, length = 50)
    private String stage; // 초심/최종심

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 판결문 내용

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "based_on", columnDefinition = "TEXT") // TEXT 타입, null 허용
    private String basedOn; // 판결 근거 데이터

    @Builder
    public Judgment(Case aCase, String stage, String content, String basedOn) {
        this.aCase = aCase;
        this.stage = stage;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.basedOn = basedOn;
    }
}