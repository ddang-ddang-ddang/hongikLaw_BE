package com.demoday.ddangddangddang.service.report;

import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.Report;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.dto.report.ReportRequestDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.DefenseRepository;
import com.demoday.ddangddangddang.repository.RebuttalRepository;
import com.demoday.ddangddangddang.repository.ReportRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import com.demoday.ddangddangddang.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final DefenseRepository defenseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final SlackNotificationService slackNotificationService;

    private static final int BLIND_THRESHOLD = 3; // 신고 임계값=3

    public void createReport(Long userId, ReportRequestDto requestDto) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        // 1. 콘텐츠 존재 여부 확인 및 내용 조회
        String reportedContent = getReportedContent(requestDto.getContentId(), requestDto.getContentType());

        // 2. 중복 신고 확인
        if (reportRepository.existsByReporterAndContentIdAndContentType(reporter, requestDto.getContentId(), requestDto.getContentType())) {
            throw new GeneralException(GeneralErrorCode.REPORT_ALREADY_EXISTS);
        }

        // 3. 신고 저장
        Report report = Report.builder()
                .reporter(reporter)
                .contentId(requestDto.getContentId())
                .contentType(requestDto.getContentType())
                .reason(requestDto.getReason())
                .customReason(requestDto.getCustomReason())
                .build();

        // saveAndFlush를 사용하여 DB에 즉시 반영 (count 쿼리 정합성 보장)
        Report savedReport = reportRepository.saveAndFlush(report);

        // 4. 현재 누적 신고 횟수 조회 (방금 저장한 것 포함)
        long currentReportCount = reportRepository.countByContentIdAndContentType(requestDto.getContentId(), requestDto.getContentType());

        // 5. Slack 알림 전송
        slackNotificationService.sendReportNotification(savedReport, reporter.getNickname(), reportedContent, currentReportCount);

        // 6. 누적 신고 횟수 확인 및 BLIND 처리
        processBlindStatus(requestDto.getContentId(), requestDto.getContentType(), currentReportCount);
    }

    /**
     * [추가] 운영자용: 허위 신고 삭제 (신고 취소)
     * 신고를 삭제하고, 신고 횟수가 임계값 미만으로 떨어지면 블라인드를 해제합니다.
     */
    public void deleteReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "존재하지 않는 신고입니다."));

        Long contentId = report.getContentId();
        ContentType contentType = report.getContentType();

        // 1. 신고 내역 삭제
        reportRepository.delete(report);

        // 2. 남은 신고 횟수 조회
        long currentCount = reportRepository.countByContentIdAndContentType(contentId, contentType);

        // 3. 블라인드 해제 여부 확인 (임계값보다 낮아지면 해제)
        if (currentCount < BLIND_THRESHOLD) {
            unblindContent(contentId, contentType);
        }
    }

    private void processBlindStatus(Long contentId, ContentType contentType) {
        // 신고 횟수 조회 (Report 엔티티의 레코드가 누적 신고 횟수)
        long reportCount = reportRepository.countByContentIdAndContentType(contentId, contentType);

        if (reportCount >= BLIND_THRESHOLD) {
            if (contentType == ContentType.DEFENSE) {
                Defense defense = defenseRepository.findById(contentId).orElse(null);
                if (defense != null && !defense.getIsBlind()) {
                    defense.markAsBlind();
                    // 추가적으로 신고자들에게 처리 결과 알림 등의 로직을 구현할 수 있습니다.
                }
            } else if (contentType == ContentType.REBUTTAL) {
                Rebuttal rebuttal = rebuttalRepository.findById(contentId).orElse(null);
                if (rebuttal != null && !rebuttal.getIsBlind()) {
                    rebuttal.markAsBlind();
                }
            }
        }
    }

    // 신고된 콘텐츠의 실제 내용을 가져오는 메서드
    private String getReportedContent(Long contentId, ContentType contentType) {
        if (contentType == ContentType.DEFENSE) {
            return defenseRepository.findById(contentId)
                    .map(Defense::getContent)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "존재하지 않는 변론입니다."));
        } else if (contentType == ContentType.REBUTTAL) {
            return rebuttalRepository.findById(contentId)
                    .map(Rebuttal::getContent)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "존재하지 않는 반론입니다."));
        } else {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 콘텐츠 타입입니다.");
        }
    }

    // 파라미터로 count를 받아서 처리하도록 최적화
    private void processBlindStatus(Long contentId, ContentType contentType, long reportCount) {
        // [로직 확인] 신고 횟수가 임계값(3) 이상이면 블라인드 처리
        if (reportCount >= BLIND_THRESHOLD) {
            if (contentType == ContentType.DEFENSE) {
                defenseRepository.findById(contentId).ifPresent(defense -> {
                    if (!defense.getIsBlind()) {
                        defense.markAsBlind();
                        // [권장] Dirty Checking에 의존하지 않고 명시적으로 저장하여 확실하게 업데이트
                        defenseRepository.save(defense);
                    }
                });
            } else if (contentType == ContentType.REBUTTAL) {
                rebuttalRepository.findById(contentId).ifPresent(rebuttal -> {
                    if (!rebuttal.getIsBlind()) {
                        rebuttal.markAsBlind();
                        // [권장] 명시적 저장
                        rebuttalRepository.save(rebuttal);
                    }
                });
            }
        }
    }

    // [추가] 블라인드 해제 로직
    private void unblindContent(Long contentId, ContentType contentType) {
        if (contentType == ContentType.DEFENSE) {
            defenseRepository.findById(contentId).ifPresent(defense -> {
                defense.unmarkAsBlind();
                defenseRepository.save(defense); // 명시적 저장
            });
        } else if (contentType == ContentType.REBUTTAL) {
            rebuttalRepository.findById(contentId).ifPresent(rebuttal -> {
                rebuttal.unmarkAsBlind();
                rebuttalRepository.save(rebuttal); // 명시적 저장
            });
        }
    }
}