package com.demoday.ddangddangddang.service.cases;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import com.demoday.ddangddangddang.repository.CaseRepository;
import com.demoday.ddangddangddang.service.third.FinalJudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgmentUpdateScheduler {

    private final CaseRepository caseRepository;
    private final FinalJudgeService finalJudgeService;

    /**
     * 매일 자정에 최종심(THIRD)이 진행 중인 모든 사건의 판결 스냅샷을 생성(아카이빙)합니다.
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 0시 0분 0초에 실행
    public void archiveDailyJudgments() {
        log.info("[Scheduler] 일일 판결 스냅샷 생성을 시작합니다.");

        // 최종심 상태인 모든 사건을 조회
        List<Case> activeCases = caseRepository.findAllByStatusOrderByCreatedAtDesc(CaseStatus.THIRD);

        if (activeCases.isEmpty()) {
            log.info("[Scheduler] 최종심(THIRD)이 진행 중인 사건이 없습니다.");
            return;
        }

        log.info("[Scheduler] 총 {}개의 최종심 사건에 대한 스냅샷 생성을 시도합니다.", activeCases.size());

        for (Case aCase : activeCases) {
            try {
                finalJudgeService.createDailyJudgmentSnapshot(aCase.getId());
            } catch (Exception e) {
                log.error("[Scheduler] Case ID {} 판결 스냅샷 생성 중 오류 발생: {}", aCase.getId(), e.getMessage(), e);
            }
        }

        log.info("[Scheduler] 일일 판결 스냅샷 생성을 완료했습니다.");
    }
}