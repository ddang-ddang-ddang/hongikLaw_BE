package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArgumentInitialRepository extends JpaRepository<ArgumentInitial,Long> {
    List<ArgumentInitial> findByUser(User user);
    // findByaCase -> findByaCaseOrderByTypeAsc (A측이 먼저 오도록 정렬)
    List<ArgumentInitial> findByaCaseOrderByTypeAsc(Case aCase);
    List<ArgumentInitial> findByaCaseInOrderByTypeAsc(List<Case> cases);
    List<ArgumentInitial> findByaCaseAndUser(Case aCase, User user);
    // 사건의 모든 1차 입장문 삭제
    void deleteAllByaCase(Case aCase);
}