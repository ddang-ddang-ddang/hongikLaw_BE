package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RebuttalRepository extends JpaRepository<Rebuttal, Integer> {
    List<Rebuttal> findRebuttalByUser(User user);
}
