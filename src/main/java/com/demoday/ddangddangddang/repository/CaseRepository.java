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

    @Query("SELECT COUNT(DISTINCT u) FROM User u WHERE " +
            // 1. 변론 작성자
            "u.id IN (SELECT d.user.id FROM Defense d WHERE d.aCase.id = :caseId) OR " +
            // 2. 반론 작성자
            "u.id IN (SELECT r.user.id FROM Rebuttal r JOIN r.defense d WHERE d.aCase.id = :caseId) OR " +
            // 3. 투표 참여자
            "u.id IN (SELECT v.user.id FROM Vote v WHERE v.aCase.id = :caseId) OR " +
            // 4. 변론 좋아요 누른 사람 (ContentType이 'DEFENSE'인 경우)
            "u.id IN (SELECT l.user.id FROM Like l WHERE l.contentType = 'DEFENSE' " +
            "         AND l.contentId IN (SELECT d.id FROM Defense d WHERE d.aCase.id = :caseId)) OR " +
            // 5. 반론 좋아요 누른 사람 (ContentType이 'REBUTTAL'인 경우)
            "u.id IN (SELECT l.user.id FROM Like l WHERE l.contentType = 'REBUTTAL' " +
            "         AND l.contentId IN (SELECT r.id FROM Rebuttal r JOIN r.defense d WHERE d.aCase.id = :caseId))")
    int countDistinctParticipants(@Param("caseId") Long caseId);
}
