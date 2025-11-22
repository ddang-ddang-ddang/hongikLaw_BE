package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Judgment;
import com.demoday.ddangddangddang.domain.enums.JudgmentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JudgmentRepository extends JpaRepository<Judgment, Long> {
    // caseId와 판결 단계(초심)로 판결문을 찾는 메서드
    Optional<Judgment> findByaCase_IdAndStage(Long caseId, JudgmentStage stage);

    Optional<Judgment> findTopByaCase_IdAndStageOrderByCreatedAtDesc(Long caseId, JudgmentStage stage);

    // 특정 사건의 FINAL 스테이지 판결을 '오래된 순'으로 모두 조회
    List<Judgment> findAllByaCase_IdAndStageOrderByCreatedAtAsc(Long caseId, JudgmentStage stage);

    int countByaCase_Id(Long aCaseId);
}