package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.DefenseLike;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DefenseLikeRepository extends JpaRepository<DefenseLike, Long> {
    Optional<DefenseLike> findByUserAndDefense(User user, Defense defense);
    boolean existsByUserAndDefense(User user, Defense defense);
    void deleteByUserAndDefense(User user, Defense defense);
}
