package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.CaseResult;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "rebuttals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Rebuttal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rebuttal_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defense_id", nullable = false)
    private Defense defense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // DB에는 parent_rebuttal_id 등으로 생성됩니다.
    private Rebuttal parent; // 부모 반론

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rebuttal> children = new ArrayList<>(); // 자식 반론들

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private DebateSide type; // 'A' 또는 'B'

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Builder.Default
    @Column(name = "is_adopted", nullable = false)
    private Boolean isAdopted = false; // 최종심 채택 여부

    @Builder.Default
    @Column(name = "case_result")
    private CaseResult caseResult = CaseResult.PENDING;

    @Builder
    public Rebuttal(Defense defense, User user, DebateSide type, String content, Rebuttal parent) {
        this.defense = defense;
        this.parent = parent;
        this.user = user;
        this.type = type;
        this.content = content;
        this.likesCount = 0;
    }

    public void markAsAdopted() {
        this.isAdopted = true;
    }

    public void incrementLikesCount() {
        this.likesCount++;
    }

    public void decrementLikesCount() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }

    // 결과 업데이트 메서드
    public void updateResult(CaseResult result) {
        this.caseResult = result;
    }
}