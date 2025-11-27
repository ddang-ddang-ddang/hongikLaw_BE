package com.demoday.ddangddangddang.global.sse;

import com.demoday.ddangddangddang.domain.Notification;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.notice.NotificationResponseDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.NotificationRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitters {

    //사건을 파라미터로 해서 연결
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    //유저 아이디를 파라미터로 해서 연결
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();
    private final NotificationRepository notificationRepository; // [추가]
    private final UserRepository userRepository; // [추가] 유저 조회를 위해 필요

    public SseEmitter add(Long caseId) {
        SseEmitter emitter = new SseEmitter(60 * 1000L * 10); // 10분 타임아웃
        this.emitters.put(caseId, emitter);
        log.info("SSE Connected: caseId={}", caseId);

        emitter.onCompletion(() -> {
            log.info("SSE Completed: caseId={}", caseId);
            this.emitters.remove(caseId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE Timeout: caseId={}", caseId);
            emitter.complete();
        });

        emitter.onError((e) -> {
            log.error("SSE Error: caseId={}, message={}", caseId, e.getMessage());
            this.emitters.remove(caseId);
        });

        return emitter;
    }

    public void send(Long caseId, Object data) {
        SseEmitter emitter = this.emitters.get(caseId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("judgment_complete")
                        .data(data));
                log.info("SSE Notification Sent: caseId={}", caseId);
                // 전송 후 연결 종료 (단발성 알림인 경우)
                emitter.complete();
                this.emitters.remove(caseId);
            } catch (IOException e) {
                log.error("SSE Send Error: caseId={}", caseId);
                this.emitters.remove(caseId);
            }
        }
    }

    // 에러 발생 시 클라이언트에게 알림
    public void sendError(Long caseId, String errorMessage) {
        SseEmitter emitter = this.emitters.get(caseId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("judgment_error")
                        .data(errorMessage));
                emitter.complete();
                this.emitters.remove(caseId);
            } catch (IOException e) {
                this.emitters.remove(caseId);
            }
        }
    }

    // 유저가 로그인 후 알림을 구독할 때 호출
    public SseEmitter connectUser(Long userId) {
        // 타임아웃을 1시간(60분)으로 설정
        SseEmitter emitter = new SseEmitter(60 * 1000L * 60);
        this.userEmitters.put(userId, emitter);
        log.info("SSE Connected User: userId={}", userId);

        // 완료 및 타임아웃 시 리스트에서 제거
        emitter.onCompletion(() -> this.userEmitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.userEmitters.remove(userId);
        });
        emitter.onError((e) -> {
            log.error("SSE Connection Error for user {}: {}", userId, e.getMessage()); // 로그 강화
            this.userEmitters.remove(userId);
        });

        // [중요] 503 Service Unavailable 방지를 위한 더미 데이터 즉시 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!"));
        } catch (IOException e) {
            log.error("SSE Initial Send Error", e);
            // 초기 전송 실패 시 이미 망가진 연결이므로 제거
            this.userEmitters.remove(userId);
        }

        return emitter;
    }

    // 특정 유저에게 알림 전송
    public void sendNotification(Long userId, String eventName, NotificationResponseDto dto) {
        log.info("알림 전송 시도 - Target UserId: {}, Event: {}", userId, eventName); // [1] 진입 로그

        // 1. DB에 알림 저장 (접속 여부와 상관없이 무조건 저장)
        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .receiver(receiver)
                .message(dto.getMessage())
                .caseId(dto.getCaseId())
                .defenseId(dto.getDefenseId())
                .rebuttalId(dto.getRebuttalId())
                .judgementId(dto.getJudgementId())
                .iconUrl(dto.getIconUrl())
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // 2. 실시간 전송 시도 (접속 중일 때만 감)
        SseEmitter emitter = this.userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(dto));
                log.info("실시간 알림 전송 성공 - UserId: {}", userId);
            } catch (IOException e) {
                this.userEmitters.remove(userId);
                log.error("실시간 전송 실패 (저장은 완료됨) - UserId: {}", userId);
            }
        }
    }

    @Scheduled(fixedRate = 45000) // 45초마다 실행 (60초 타임아웃 방지)
    public void sendHeartbeat() {
        userEmitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("keep-alive"));
            } catch (IOException e) {
                // 전송 실패 시(클라이언트가 이미 떠남) 제거
                userEmitters.remove(userId);
            }
        });
    }
}
