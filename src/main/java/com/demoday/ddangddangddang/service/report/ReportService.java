package com.demoday.ddangddangddang.service.report;

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

    public void createReport(Long userId, ReportRequestDto requestDto) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        // 1. 콘텐츠 존재 여부 확인
        validateContentExists(requestDto.getContentId(), requestDto.getContentType());

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

        reportRepository.save(report);
    }

    private void validateContentExists(Long contentId, ContentType contentType) {
        if (contentType == ContentType.DEFENSE) {
            if (!defenseRepository.existsById(contentId)) {
                throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "존재하지 않는 변론입니다.");
            }
        } else if (contentType == ContentType.REBUTTAL) {
            if (!rebuttalRepository.existsById(contentId)) {
                throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "존재하지 않는 반론입니다.");
            }
        } else {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 콘텐츠 타입입니다.");
        }
    }
}