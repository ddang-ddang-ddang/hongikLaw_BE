package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.CaseResult;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RebuttalRepository extends JpaRepository<Rebuttal, Long> {
    List<Rebuttal> findRebuttalByUser(User user);

    // 특정 사건(Case) ID에 속한 모든 반론(Rebuttal)들을 조회
    List<Rebuttal> findAllByDefense_aCase_Id(Long caseId);

    // 특정 변론(Defense)에 속한 모든 반론(Rebuttal)들을 조회
    List<Rebuttal> findAllByDefense_Id(Long defenseId);

    @Modifying
    @Query("UPDATE Rebuttal r SET r.likesCount = r.likesCount + 1 WHERE r.id = :rebuttalId")
    void incrementLikesCount(@Param("rebuttalId") Long rebuttalId);

    @Modifying
    @Query("UPDATE Rebuttal r SET r.likesCount = r.likesCount - 1 WHERE r.id = :rebuttalId")
    void decrementLikesCount(@Param("rebuttalId") Long rebuttalId);

    //채택된 변론
    List<Rebuttal> findByIsAdopted(Boolean isAdopted);

    //사건별 채택된 반론
    @Query("SELECT r FROM Rebuttal r " +
            "JOIN r.defense d " +
            "WHERE d.aCase.id = :caseId AND r.isAdopted = true AND r.isBlind = false") // ✨ isBlind = false 조건 추가
    List<Rebuttal> findAdoptedRebuttalsByCaseId(@Param("caseId") Long caseId);

    //변론당 좋아요 많은 반론
    List<Rebuttal> findByDefense_IdOrderByLikesCountDesc(Long defenseId);

    //사건별 좋아요가 많은 변론 상위 5개
    List<Rebuttal> findTop5ByDefense_aCase_IdAndTypeAndIsBlindFalseOrderByLikesCountDesc(Long caseId, DebateSide type); // ✨ 메서드 이름 수정

    // [수정] FinalJudgeService 사용. BLIND 미포함
    List<Rebuttal> findAllByDefense_aCase_IdAndTypeAndIsBlindFalse(Long caseId, DebateSide type); // ✨ 메서드 이름 수정

    Integer countByUser(User user);

    Integer countByUserAndCaseResult(User user, CaseResult caseResult);

    Integer countByUserAndIsAdopted(User user, Boolean isAdopted);

    List<Rebuttal> findByUser(User user);

    @Query("SELECT COALESCE(SUM(r.likesCount), 0) FROM Rebuttal r WHERE r.user = :user")
    Integer sumLikesByUser(@Param("user") User user);
}
