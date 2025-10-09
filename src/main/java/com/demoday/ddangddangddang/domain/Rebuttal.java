package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.AgreeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rebuttals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rebuttal {

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

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "agree_status", nullable = false)
    private AgreeStatus agreeStatus;

    @Builder
    public Rebuttal(Defense defense, Rebuttal parent, User user, AgreeStatus agreeStatus, String content) {
        this.defense = defense;
        this.parent = parent;
        this.user = user;
        this.agreeStatus = agreeStatus;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}