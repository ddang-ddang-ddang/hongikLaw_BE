package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefenseRepository extends JpaRepository<Defense,Long>{
    // [Fix] MyPage/본인 이력 조회를 위해 원본 메서드를 유지
    List<Defense> findDefenseByUser(User user);

    // --- [ 1. 이 메서드를 수정합니다 ] ---
    // [수정] DebateService 사용. BLIND 미포함
    List<Defense> findAllByaCase_IdAndIsBlindFalse(Long caseId);

    // --- [ 2. 이 메서드를 수정합니다 ] ---
    // [수정] 추천수 상위 N개 조회. BLIND 미포함
    List<Defense> findTop5ByaCase_IdAndIsBlindFalseOrderByLikesCountDesc(Long caseId);
    // ---------------------------------

    @Modifying // (1) 이 쿼리가 DB를 수정함을 알림
    @Query("UPDATE Defense d SET d.likesCount = d.likesCount + 1 WHERE d.id = :defenseId")
    void incrementLikesCount(@Param("defenseId") Long defenseId);

    @Modifying
    @Query("UPDATE Defense d SET d.likesCount = d.likesCount - 1 WHERE d.id = :defenseId")
    void decrementLikesCount(@Param("defenseId") Long defenseId);

    // [수정] 채택된 변론. BLIND 미포함 (Fixes FinalJudgeService, AdoptService)
    List<Defense> findByaCase_IdAndIsAdoptedAndIsBlindFalse(Long caseId, Boolean isAdopted);

    // [수정] 좋아요 높은 순. BLIND 미포함
    List<Defense> findAllByaCase_IdAndIsBlindFalseOrderByLikesCountDesc(Long caseId);

    // [수정] 좋아요 높은 순 Top 10. BLIND 미포함
    List<Defense> findTop10ByaCase_IdAndIsBlindFalseOrderByLikesCountDesc(Long caseId);

    // [수정] 좋아요 높은 순 + 진영. BLIND 미포함
    List<Defense> findTop5ByaCase_IdAndTypeAndIsBlindFalseOrderByLikesCountDesc(Long caseId, DebateSide type);

    // [수정] 사건별 + 진영별 모든 변론. BLIND 미포함 (Fixes AdoptService, FinalJudgeService)
    List<Defense> findAllByaCase_IdAndTypeAndIsBlindFalse(Long caseId, DebateSide type);
}