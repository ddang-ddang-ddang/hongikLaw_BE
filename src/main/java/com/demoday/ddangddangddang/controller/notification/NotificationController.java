package com.demoday.ddangddangddang.controller.notification;

import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "알림 관련 컨트롤러 -by 황신애")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SseEmitters sseEmitters;
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SecurityRequirement(name = "JWT TOKEN")
    @Operation(summary = "알림 구독", description = "알림을 전송합니다")
    public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        SseEmitter emitter = sseEmitters.connectUser(userDetails.getUser().getId());

        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no") // Nginx 버퍼링 방지 (중요!)
                .header("Connection", "keep-alive") // 연결 유지
                .body(emitter);
    }
}
