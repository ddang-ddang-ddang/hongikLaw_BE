package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Report;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 이미 신고했는지 확인
    boolean existsByReporterAndContentIdAndContentType(User reporter, Long contentId, ContentType contentType);
    // 특정 콘텐츠의 누적 신고 횟수 조회
    long countByContentIdAndContentType(Long contentId, ContentType contentType);
    // 특정 콘텐츠의 모든 신고 내역 삭제 (삭제 시 사용)
    void deleteByContentIdAndContentType(Long contentId, ContentType contentType);
}