package com.demoday.ddangddangddang.service.mainpage;

import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.repository.CaseParticipationRepository;
import com.demoday.ddangddangddang.repository.DefenseRepository;
import com.demoday.ddangddangddang.repository.RebuttalRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MainpageService {
    private final UserRepository userRepository;
    private final CaseParticipationRepository caseParticipationRepository;
    private final DefenseRepository defenseRepository;
    private final RebuttalRepository rebuttalRepository;

    //현재 핫한 재판

    //진행중인 재판
    public ApiResponse<> getCaseList(Long userId){

    }
    //변호 이력
}
