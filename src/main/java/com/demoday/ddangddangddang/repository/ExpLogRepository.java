package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.ExpLog;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpLogRepository extends JpaRepository<ExpLog, Long> {
    // 특정 유저의 경험치 내역을 생성일자 내림차순(최신순)으로 조회
    List<ExpLog> findAllByUserOrderByCreatedAtDesc(User user);
}