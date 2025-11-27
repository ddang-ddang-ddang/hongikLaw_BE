package com.demoday.ddangddangddang.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suggestions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Suggestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suggestion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 건의한 사람

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 건의 내용

    @Builder
    public Suggestion(User user, String content) {
        this.user = user;
        this.content = content;
    }
}