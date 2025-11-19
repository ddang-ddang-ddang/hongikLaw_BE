package com.demoday.ddangddangddang.controller.notification;

import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Tag(name = "notice controller", description = "알림 관련 컨트롤러 -by 황신애")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SseEmitters sseEmitters;

    // 클라이언트에서 EventSource로 연결할 엔드포인트
    @Operation(summary = "알림 구독", description = "알림을 전송합니다")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return sseEmitters.connectUser(userDetails.getUser().getId());
    }
}
