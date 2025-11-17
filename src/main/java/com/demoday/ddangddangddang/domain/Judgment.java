package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.JudgmentStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "judgments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Judgment extends BaseEntity {

    // ... (id, aCase, stage, createdAt, based_on, ratio_a, ratio_b 필드 동일) ...
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "judgment_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 50)
    private JudgmentStage stage; // 'INITIAL' 또는 'FINAL'

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 판결문 내용

    //삭제
    //@CreationTimestamp
    //@Column(name = "created_at", nullable = false, updatable = false)
    //private LocalDateTime createdAt;

    @Column(name = "based_on", columnDefinition = "TEXT")
    private String basedOn; // 판결 근거 데이터

    @Column(name = "ratio_a", nullable = false)
    private Integer ratioA;

    @Column(name = "ratio_b", nullable = false)
    private Integer ratioB;

    @Builder
    public Judgment(Case aCase, JudgmentStage stage, String content, String basedOn, Integer ratioA, Integer ratioB) {
        this.aCase = aCase;
        this.stage = stage;
        this.content = content;
        //this.createdAt = LocalDateTime.now(); // [수정] createdAt은 @CreatedDate가 BaseEntity에 있으므로 제거 가능 (BaseEntity 상속 시)
        this.basedOn = basedOn;
        this.ratioA = ratioA;
        this.ratioB = ratioB;
    }

    // --- [ 1. 이 메서드를 추가합니다 ] ---
    // AI 판결 결과를 덮어쓰기 위한 메서드
    public void updateJudgment(String content, String basedOn, Integer ratioA, Integer ratioB) {
        this.content = content;
        this.basedOn = basedOn;
        this.ratioA = ratioA;
        this.ratioB = ratioB;
    }
}