package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.CaseParticipation;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.CaseResult;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseParticipationRepository extends JpaRepository<CaseParticipation, Long> {
    public List<CaseParticipation> findByUser(User user);

    List<CaseParticipation> findByUserAndResult(User user, CaseResult caseResult);

    List<CaseParticipation> findByaCase(Case aCase);

    Integer countByUser(User user);
}
