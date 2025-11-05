package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.DebateSide;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "defenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Defense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defense_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private DebateSide type;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Builder.Default
    @Column(name = "is_adopted", nullable = false)
    private Boolean isAdopted = false;

    // ✅ 생성자 @Builder 제거 (중복 방지)
    public Defense(Case aCase, User user, DebateSide type, String title, String content) {
        this.aCase = aCase;
        this.user = user;
        this.type = type;
        this.content = content;
    }

    public void markAsAdopted() { this.isAdopted = true; }

    public void incrementLikesCount() { this.likesCount++; }

    public void decrementLikesCount() {
        if (this.likesCount > 0) this.likesCount--;
    }
}
