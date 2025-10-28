package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.DebateSide;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private DebateSide type; // 'A' 또는 'B'

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Rebuttal(Defense defense, User user, DebateSide type, String content, Rebuttal parent) {
        this.defense = defense;
        this.parent = parent;
        this.user = user;
        this.type = type;
        this.content = content;
        this.likesCount = 0;
        this.createdAt = LocalDateTime.now();
    }
}