package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUser(User user);
}
