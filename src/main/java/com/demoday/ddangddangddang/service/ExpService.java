package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.ExpLog;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.repository.ExpLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpService {
    private final ExpLogRepository expLogRepository;

    // 이 메서드를 user.addExp() 대신 사용하세요
    public void addExp(User user, Long amount, String description) {
        // 1. 실제 유저 경험치 증가
        user.addExp(amount);

        // 2. 내역 저장
        ExpLog log = ExpLog.builder()
                .user(user)
                .amount(amount)
                .description(description)
                .build();
        expLogRepository.save(log);
    }
}