package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefenseRepository extends JpaRepository<Defense,Long>{
    List<Defense> findDefenseByUser(User user);

    @Modifying // (1) 이 쿼리가 DB를 수정함을 알림
    @Query("UPDATE Defense d SET d.likesCount = d.likesCount + 1 WHERE d.id = :defenseId")
    void incrementLikesCount(@Param("defenseId") Long defenseId);

    @Modifying
    @Query("UPDATE Defense d SET d.likesCount = d.likesCount - 1 WHERE d.id = :defenseId")
    void decrementLikesCount(@Param("defenseId") Long defenseId);
}
