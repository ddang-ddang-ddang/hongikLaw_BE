package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.Vote;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote,Long> {
    // 최신 투표 내역 1건만 조회
    Optional<Vote> findTopByaCase_IdAndUser_IdOrderByVotedAtDesc(Long caseId, Long userId);
    // A측 투표 수 계산
    long countByaCase_IdAndType(Long caseId, DebateSide type);

    List<Vote> findByaCase_IdAndType(Long caseId, DebateSide type);
    // 사건에 속한 모든 투표 삭제
    void deleteAllByaCase(Case aCase);
}