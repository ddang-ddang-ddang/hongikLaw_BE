package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.Vote;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote,Long> {
    // --- [ 1. 이 메서드들을 추가합니다 ] ---

    // 사용자와 사건 ID로 투표 기록 조회 (중복 투표 방지 및 수정용)
    Optional<Vote> findByaCase_IdAndUser_Id(Long caseId, Long userId);

    // A측 투표 수 계산
    long countByaCase_IdAndType(Long caseId, DebateSide type);
    // ------------------------------------
}