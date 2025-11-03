package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Judgment;
import com.demoday.ddangddangddang.domain.enums.JudgmentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JudgmentRepository extends JpaRepository<Judgment, Long> {
    // caseId와 판결 단계(초심)로 판결문을 찾는 메서드
    Optional<Judgment> findByaCase_IdAndStage(Long caseId, JudgmentStage stage);
}