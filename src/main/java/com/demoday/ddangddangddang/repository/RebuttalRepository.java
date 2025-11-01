package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RebuttalRepository extends JpaRepository<Rebuttal, Integer> {
    List<Rebuttal> findRebuttalByUser(User user);

    @Modifying
    @Query("UPDATE Rebuttal r SET r.likesCount = r.likesCount + 1 WHERE r.id = :rebuttalId")
    void incrementLikesCount(@Param("rebuttalId") Long rebuttalId);

    @Modifying
    @Query("UPDATE Rebuttal r SET r.likesCount = r.likesCount - 1 WHERE r.id = :rebuttalId")
    void decrementLikesCount(@Param("rebuttalId") Long rebuttalId);
}
