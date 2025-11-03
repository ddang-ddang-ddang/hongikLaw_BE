package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class FinalJudgeService {
    private final CaseRepository caseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final DefenseRepository defenseRepository;
    private final UserRepository userRepository;
    private final JudgmentRepository JudgmentRepository;

    //판결문 저장

    //판결문 조회
}
