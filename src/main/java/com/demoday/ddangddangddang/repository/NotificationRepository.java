package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Notification;
import com.demoday.ddangddangddang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 유저의 읽지 않은 알림을 최신순으로 조회
    List<Notification> findAllByReceiverAndIsReadFalseOrderByCreatedAtDesc(User receiver);
}
