package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.Report;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    // ObjectMapper는 다른 Config(OpenAiConfig)에서 Bean으로 정의되어 있으므로 재사용
    private final ObjectMapper objectMapper;

    // Java 11+ HttpClient 사용
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Slack Webhook으로 신고 알림을 전송합니다. (비동기)
     */
    public void sendReportNotification(Report report, String reporterNickname) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            log.warn("Slack Webhook URL이 설정되지 않아 신고 알림을 건너뜁니다.");
            return;
        }

        String messageText = buildSlackMessage(report, reporterNickname);

        try {
            // Slack 메시지 payload (JSON 형식)
            String jsonPayload = objectMapper.writeValueAsString(Map.of("text", messageText));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(slackWebhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // 비동기로 전송하고 결과를 로그로 확인
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            log.error("Slack 알림 전송 실패. 응답 코드: {}, 본문: {}", response.statusCode(), response.body());
                        } else {
                            log.info("Slack 알림 성공적으로 전송 완료: {}", report.getId());
                        }
                    })
                    .exceptionally(e -> {
                        log.error("Slack 알림 전송 중 예외 발생: {}", e.getMessage(), e);
                        return null;
                    });

        } catch (IOException e) {
            log.error("Slack 메시지 직렬화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * Slack 메시지 본문 생성 (Markdown 포맷)
     */
    private String buildSlackMessage(Report report, String reporterNickname) {
        String contentInfo = String.format("%s (ID: %d)", report.getContentType().name(), report.getContentId());
        String reasonDetail = report.getReason().getDescription();

        return String.format(
                "🚨 *새로운 콘텐츠 신고 접수* 🚨\n" +
                        "-----------------------------------\n" +
                        "• 신고 ID: `%d`\n" +
                        "• 신고 콘텐츠: `%s`\n" +
                        "• 신고자: `%s` (ID: %d)\n" +
                        "• 신고 사유: *%s*\n" +
                        "• 상세 사유: %s\n" +
                        "• 접수 시각: %s\n" +
                        "-----------------------------------",
                report.getId(),
                contentInfo,
                reporterNickname,
                report.getReporter().getId(),
                reasonDetail,
                report.getCustomReason() != null && !report.getCustomReason().isEmpty() ? report.getCustomReason() : "없음",
                report.getCreatedAt() != null ? report.getCreatedAt().format(FORMATTER) : LocalDateTime.now().format(FORMATTER)
        );
    }
}