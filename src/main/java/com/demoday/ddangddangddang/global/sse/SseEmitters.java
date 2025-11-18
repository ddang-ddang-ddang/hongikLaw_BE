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

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

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
}
