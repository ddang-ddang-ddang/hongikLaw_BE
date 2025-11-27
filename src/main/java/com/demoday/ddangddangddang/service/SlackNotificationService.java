package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.Report;
import com.demoday.ddangddangddang.domain.Suggestion;
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

    // 1. 기존 신고용 웹훅 URL (운영-신고-알림 채널)
    @Value("${slack.webhook.url}")
    private String slackReportUrl;

    // 2. 새로운 건의사항용 웹훅 URL (운영-건의-알림 채널)
    @Value("${slack.webhook.suggestion-url}")
    private String slackSuggestionUrl;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * [신고 알림 전송]
     * 기존 로직 유지: 신고용 URL 사용
     */
    public void sendReportNotification(Report report, String reporterNickname, String content, long reportCount) {
        // 메시지 생성
        String messageText = buildReportMessage(report, reporterNickname, content, reportCount);

        // 공통 전송 메서드 호출 (신고용 URL)
        sendSlackMessage(slackReportUrl, messageText, "신고 알림");
    }

    /**
     * [건의사항 알림 전송]
     * 신규 로직: 건의사항용 URL 사용
     */
    public void sendSuggestionNotification(Suggestion suggestion) {
        // 메시지 생성
        String messageText = buildSuggestionMessage(suggestion);

        // 공통 전송 메서드 호출 (건의사항용 URL)
        sendSlackMessage(slackSuggestionUrl, messageText, "건의 알림");
    }

    /**
     * [공통] 실제 슬랙으로 HTTP 요청을 보내는 메서드
     * URL과 메시지만 다르고 전송 로직은 동일하므로 하나로 통합합니다.
     */
    private void sendSlackMessage(String webhookUrl, String messageText, String logPrefix) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("[Slack] {} URL이 설정되지 않아 전송을 건너뜁니다.", logPrefix);
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(Map.of("text", messageText));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            log.error("[Slack] {} 전송 실패. 응답 코드: {}, URL: {}", logPrefix, response.statusCode(), webhookUrl);
                        } else {
                            log.info("[Slack] {} 전송 성공", logPrefix);
                        }
                    })
                    .exceptionally(e -> {
                        log.error("[Slack] {} 전송 중 예외 발생: {}", logPrefix, e.getMessage());
                        return null;
                    });

        } catch (IOException e) {
            log.error("[Slack] 메시지 직렬화 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * [포맷팅] 신고 메시지 본문 생성
     */
    private String buildReportMessage(Report report, String reporterNickname, String content, long reportCount) {
        String contentInfo = String.format("%s (ID: %d)", report.getContentType().name(), report.getContentId());
        String reasonDetail = report.getReason().getDescription();

        // 내용이 너무 길면 잘라서 표시
        String displayContent = content.length() > 100 ? content.substring(0, 100) + "..." : content;

        return String.format(
                "🚨 *새로운 콘텐츠 신고 접수* 🚨\n" +
                        "-----------------------------------\n" +
                        "• 신고 ID: `%d`\n" +
                        "• 신고 대상: `%s`\n" +
                        "• 신고자: `%s` (ID: %d)\n" +
                        "• 누적 신고 수: *%d회* (3회 이상 시 블라인드)\n" +
                        "-----------------------------------\n" +
                        "• 신고 사유: *%s*\n" +
                        "• 상세 사유: %s\n" +
                        "• 신고 내용: \n> %s\n" +
                        "-----------------------------------\n" +
                        "• 접수 시각: %s\n",
                report.getId(),
                contentInfo,
                reporterNickname,
                report.getReporter().getId(),
                reportCount,
                reasonDetail,
                report.getCustomReason() != null && !report.getCustomReason().isEmpty() ? report.getCustomReason() : "없음",
                displayContent,
                report.getCreatedAt() != null ? report.getCreatedAt().format(FORMATTER) : LocalDateTime.now().format(FORMATTER)
        );
    }

    /**
     * [포맷팅] 건의사항 메시지 본문 생성
     */
    private String buildSuggestionMessage(Suggestion suggestion) {
        return String.format(
                "💡 *새로운 건의사항 도착* 💡\n" +
                        "-----------------------------------\n" +
                        "• 건의 ID: `%d`\n" +
                        "• 작성자: `%s` (ID: %d)\n" +
                        "-----------------------------------\n" +
                        "• 내용: \n> %s\n" +
                        "-----------------------------------\n" +
                        "• 접수 시각: %s\n",
                suggestion.getId(),
                suggestion.getUser().getNickname(),
                suggestion.getUser().getId(),
                suggestion.getContent(),
                suggestion.getCreatedAt() != null ? suggestion.getCreatedAt().format(FORMATTER) : LocalDateTime.now().format(FORMATTER)
        );
    }
}