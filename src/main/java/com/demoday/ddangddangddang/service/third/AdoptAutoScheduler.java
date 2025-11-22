package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import com.demoday.ddangddangddang.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdoptAutoScheduler {

    private final AdoptService adoptService;
    private final CaseRepository caseRepository;

    //매일 자정마다 투표기간이 끝난지 1일이 지난 사건들을 third로 바꾸고 자동채택
    @Scheduled(cron = "0 30 23 * * *")
    public void autoAdoptScheduled(){
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        List<Case> cases = caseRepository.findByAppealDeadlineBeforeAndStatus(oneDayAgo, CaseStatus.SECOND);
        if (cases.isEmpty()) {
            log.info("대상 사건 없음");
            return;
        }

        for (Case aCase : cases) {
            try {
                // 시스템 자동 채택 수행 (내부에서 상태 변경 setThird() 포함)
                adoptService.executeSystemAutoAdopt(aCase);

                log.info("사건 ID {} 자동 채택 및 상태 변경 완료", aCase.getId());
            } catch (Exception e) {
                log.error("사건 ID {} 처리 중 오류 발생: {}", aCase.getId(), e.getMessage());
            }
        }
    }
}
