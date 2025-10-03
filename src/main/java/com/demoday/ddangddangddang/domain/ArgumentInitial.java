package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.AgreeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "argument_initials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArgumentInitial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "argument_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase; // 'case'는 자바 예약어이므로 'aCase' 등으로 명명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "agree_status", nullable = false)
    private AgreeStatus agreeStatus; // 찬성/반대

    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT") // TEXT 타입 명시
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ArgumentInitial(Case aCase, User user, AgreeStatus agreeStatus, String title, String content) {
        this.aCase = aCase;
        this.user = user;
        this.agreeStatus = agreeStatus;
        this.title = title;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}