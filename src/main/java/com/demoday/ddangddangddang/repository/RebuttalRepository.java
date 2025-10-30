package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Rebuttal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RebuttalRepository extends JpaRepository<Rebuttal, Integer> {
}
