package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.CaseResult;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "case_participations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class CaseParticipation {
    //유저와 사건 중간 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseResult result; // 해당 사건에서 이 유저의 결과 (WIN, LOSE, PENDING)

    @Builder
    public CaseParticipation(User user, Case aCase) {
        this.user = user;
        this.aCase = aCase;
        this.result = CaseResult.PENDING; // 처음 생성 시에는 '진행 중' 상태
    }

    // 결과 업데이트 메서드
    public void updateResult(CaseResult result) {
        this.result = result;
    }
}
