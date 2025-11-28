package com.demoday.ddangddangddang.controller.notification;

import com.demoday.ddangddangddang.domain.Notification;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.notice.NotificationResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.NotificationRepository;
import com.demoday.ddangddangddang.service.NotificatoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "알림 관련 컨트롤러 -by 황신애")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    private final SseEmitters sseEmitters;
    private final NotificatoinService notificatoinService;

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

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "읽지 않은 알림 목록을 조회합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();

        // 읽지 않은 알림 조회
        List<Notification> notifications = notificationRepository.findAllByReceiverAndIsReadFalseOrderByCreatedAtDesc(user);

        // DTO 변환
        List<NotificationResponseDto> responseDtos = notifications.stream()
                .map(n -> NotificationResponseDto.builder()
                        .message(n.getMessage())
                        .id(n.getId())
                        .caseId(n.getCaseId())
                        .defenseId(n.getDefenseId())
                        .rebuttalId(n.getRebuttalId())
                        .judgementId(n.getJudgementId())
                        .iconUrl(n.getIconUrl())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("알림 목록 조회 성공", responseDtos);
    }

    @PatchMapping("/{notiId}")
    @Operation(summary = "알림 읽음", description = "읽음 처리")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<Void> readNotification(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long notiId) {
        Long userId = userDetails.getUser().getId();

        return notificatoinService.readNotification(userId,notiId);
    }

    @DeleteMapping("/{notiId}")
    @Operation(summary = "알림 삭제")
    public ApiResponse<Void> deleteNotification(@PathVariable Long notiId) {
        return notificatoinService.deleteNotification(notiId);
    }
}
