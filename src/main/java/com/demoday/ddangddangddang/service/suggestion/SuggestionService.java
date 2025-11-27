package com.demoday.ddangddangddang.service.suggestion;

import com.demoday.ddangddangddang.domain.Suggestion;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.suggestion.SuggestionRequestDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.SuggestionRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import com.demoday.ddangddangddang.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final SlackNotificationService slackNotificationService;

    public void createSuggestion(Long userId, SuggestionRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        // 1. 건의사항 DB 저장
        Suggestion suggestion = Suggestion.builder()
                .user(user)
                .content(requestDto.getContent())
                .build();

        Suggestion savedSuggestion = suggestionRepository.save(suggestion);

        // 2. Slack 알림 전송
        slackNotificationService.sendSuggestionNotification(savedSuggestion);
    }
}