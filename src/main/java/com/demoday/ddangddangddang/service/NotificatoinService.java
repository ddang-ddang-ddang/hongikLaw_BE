package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.Notification;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.repository.NotificationRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

@Service
@RequiredArgsConstructor
public class NotificatoinService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public ApiResponse<Void> readNotification(Long userId, Long notiId) {
        User user = userRepository.findById(userId).
                orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Notification notification = notificationRepository.findById(notiId).
                orElseThrow(()-> new GeneralException(GeneralErrorCode.FORBIDDEN));

        notification.markAsRead();

        return ApiResponse.onSuccess("알림 읽음 처리 완료");
    }

    public ApiResponse<Void> deleteNotification(Long notiId){
        Notification notification = notificationRepository.findById(notiId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN));

        notificationRepository.delete(notification);

        return ApiResponse.onSuccess("알림 삭제 완료");
    }
}
