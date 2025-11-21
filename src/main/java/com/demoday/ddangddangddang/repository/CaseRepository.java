package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case,Long> {
    // PENDING 상태인 사건들을 생성일자 내림차순(최신순)으로 조회
    List<Case> findAllByStatusOrderByCreatedAtDesc(CaseStatus status);

    List<Case> findByAppealDeadlineBeforeAndStatus(LocalDateTime threshold, CaseStatus status);

    @Query("SELECT COUNT(DISTINCT u) FROM User u " +
            "WHERE u.id IN (SELECT d.user.id FROM Defense d WHERE d.aCase.id = :caseId) " +
            "OR u.id IN (SELECT r.user.id FROM Rebuttal r JOIN r.defense d WHERE d.aCase.id = :caseId)")
    int countDistinctParticipants(@Param("caseId") Long caseId);
}
