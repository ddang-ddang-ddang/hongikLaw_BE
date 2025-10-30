package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Judgment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JudgementRepository extends JpaRepository<Judgment,Long> {
}
