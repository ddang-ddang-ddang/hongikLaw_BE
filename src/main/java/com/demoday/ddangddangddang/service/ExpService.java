package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.ExpLog;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.ExpLogRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpService {
    private final ExpLogRepository expLogRepository;
    private final UserRepository userRepository;

    public void addExp(User user, Long amount, String description) {
        // ID로 유저를 다시 조회하여 영속 상태(Managed)로 만듦 + 비관적 락 적용 권장
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        // 1. 영속 상태인 객체의 값을 변경 (트랜잭션 종료 시 자동 Update됨)
        managedUser.addExp(amount);

        // 2. 내역 저장
        ExpLog log = ExpLog.builder()
                .user(managedUser) // 연관관계도 managedUser로 맺는 것이 안전
                .amount(amount)
                .description(description)
                .build();
        expLogRepository.save(log);
    }
}