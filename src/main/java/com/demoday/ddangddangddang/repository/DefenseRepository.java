package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefenseRepository extends JpaRepository<Defense,Long>{
    List<Defense> findDefenseByUser(User user);
}
