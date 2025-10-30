package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Defense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefenseRepository extends JpaRepository<Defense,Long>{
}
