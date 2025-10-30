package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.AgreeStatus;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
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
public class ArgumentInitial extends BaseEntity {

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
    @Column(name = "type", nullable = false)
    private DebateSide type; // 찬성/반대

    @Column(name = "main_argument", nullable = false, columnDefinition = "TEXT")
    private String mainArgument;

    @Column(name = "reasoning", nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @Builder
    public ArgumentInitial(Case aCase, User user, DebateSide type, String mainArgument, String reasoning) {
        this.aCase = aCase;
        this.user = user;
        this.mainArgument = mainArgument;
        this.reasoning = reasoning;
        this.type = type;
    }
}