package com.demoday.ddangddangddang.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitters {

    //사건을 파라미터로 해서 연결
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    //유저 아이디를 파라미터로 해서 연결
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

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
        // 타임아웃: 1시간 (필요에 따라 조정)
        SseEmitter emitter = new SseEmitter(60 * 1000L * 60);
        this.userEmitters.put(userId, emitter);
        log.info("SSE Connected User: userId={}", userId);

        emitter.onCompletion(() -> this.userEmitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.userEmitters.remove(userId);
        });
        emitter.onError((e) -> this.userEmitters.remove(userId));

        // 연결 확인용 더미 데이터 전송 (연결 즉시 안 보내면 타임아웃 나는 브라우저 이슈 방지)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            log.error("SSE Connect Error", e);
        }

        return emitter;
    }

    // 특정 유저에게 알림 전송
    public void sendNotification(Long userId, String eventName, String message) {
        SseEmitter emitter = this.userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName) // 예: "new_participant", "new_rebuttal"
                        .data(message));
            } catch (IOException e) {
                this.userEmitters.remove(userId);
                log.error("SSE Notification Send Error: userId={}", userId);
            }
        }
    }
}
