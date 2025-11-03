package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArgumentInitialRepository extends JpaRepository<ArgumentInitial,Long> {
    public List<ArgumentInitial> findByUser(User user);

    List<ArgumentInitial> findByaCase(Case aCase);
}
