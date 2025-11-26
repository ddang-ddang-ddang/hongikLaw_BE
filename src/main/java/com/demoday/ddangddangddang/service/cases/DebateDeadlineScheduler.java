package com.demoday.ddangddangddang.service.cases;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import com.demoday.ddangddangddang.dto.notice.NotificationResponseDto;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.ArgumentInitialRepository;
import com.demoday.ddangddangddang.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebateDeadlineScheduler {

    private final CaseRepository caseRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final SseEmitters sseEmitters;
    private final StringRedisTemplate redisTemplate;

    private static final String NOTI_KEY_PREFIX = "notification:appeal_end:";

    // 1분마다 실행하여 마감된 사건 체크
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void checkAndNotifyAppealDeadlines() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 마감 시간이 지났지만 아직 2차 재판(SECOND) 상태인 사건 조회
        List<Case> expiredCases = caseRepository.findByAppealDeadlineBeforeAndStatus(now, CaseStatus.SECOND);

        for (Case aCase : expiredCases) {
            String redisKey = NOTI_KEY_PREFIX + aCase.getId();

            // 2. 이미 알림을 보냈는지 Redis에서 확인 (중복 발송 방지)
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                continue;
            }

            // 3. 알림 발송 대상 조회 (사건 당사자들)
            // 1차 입장문을 작성한 유저들이 해당 사건의 당사자(채택 권한자)입니다.
            List<ArgumentInitial> participants = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

            for (ArgumentInitial participant : participants) {
                Long userId = participant.getUser().getId();

                // 알림 DTO 생성
                NotificationResponseDto notificationDto = NotificationResponseDto.builder()
                        .caseId(aCase.getId())
                        .message("투표가 종료되었습니다! 최종 판결을 위해 의견을 채택해주세요.")
                        .iconUrl("https://ddangddangddang-demoday.s3.ap-northeast-2.amazonaws.com/icons/vote_end.png")
                        .build();

                // 4. SSE 알림 전송
                sseEmitters.sendNotification(userId, "notification", notificationDto);
                log.info("투표 종료 알림 전송 완료: CaseId={}, UserId={}", aCase.getId(), userId);
            }

            // 5. 알림 발송 처리 표시 (Redis에 저장, 유효기간 24시간)
            // 24시간 뒤에는 자동 채택 스케줄러가 돌거나 상태가 바뀌므로 키가 만료되어도 안전함
            redisTemplate.opsForValue().set(redisKey, "SENT", 24, TimeUnit.HOURS);
        }
    }
}