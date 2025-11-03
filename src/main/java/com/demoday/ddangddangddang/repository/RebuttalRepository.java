package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RebuttalRepository extends JpaRepository<Rebuttal, Long> {
    List<Rebuttal> findRebuttalByUser(User user);

    @Modifying
    @Query("UPDATE Rebuttal r SET r.likesCount = r.likesCount + 1 WHERE r.id = :rebuttalId")
    void incrementLikesCount(@Param("rebuttalId") Long rebuttalId);

    @Modifying
    @Query("UPDATE Rebuttal r SET r.likesCount = r.likesCount - 1 WHERE r.id = :rebuttalId")
    void decrementLikesCount(@Param("rebuttalId") Long rebuttalId);

    //채택된 변론
    List<Rebuttal> findByIsAdopted(Boolean isAdopted);

    //사건별 채택된 변론
    @Query("SELECT r FROM Rebuttal r " +
            "JOIN r.defense d " + // Rebuttal(r)과 Defense(d)를 조인
            "WHERE d.aCase.id = :caseId AND r.isAdopted = true")
    List<Rebuttal> findAdoptedRebuttalsByCaseId(@Param("caseId") Long caseId);

    //반론 좋아요 많은 수
    List<Rebuttal> findAllByOrderByLikesCountDesc(Long caseId);

    //변론당 좋아요 많은 반론
    List<Rebuttal> findByDefense_IdOrderByLikesCountDesc(Long defenseId);
}
