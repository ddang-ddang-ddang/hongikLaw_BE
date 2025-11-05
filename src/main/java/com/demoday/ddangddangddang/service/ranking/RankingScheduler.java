package com.demoday.ddangddangddang.service.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String HOT_CASES_KEY = "hot_cases";
    private static final double DECAY_FACTOR = 0.95; // 1시간마다 5%씩 점수 감소
    private static final double MIN_SCORE_THRESHOLD = 1.0; // 최소 점수 (이하이면 삭제)

    /**
     * 매시간 정각에 실행 (예: 01:00, 02:00)
     * (fixedRate = 3600000 는 1시간마다)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void decayHotCaseScores() {
        log.info("'핫한 재판' 랭킹 점수 감소 스케줄러 시작");

        // 1. 랭킹에 있는 모든 재판(Member)과 점수(Score)를 가져옴
        Set<ZSetOperations.TypedTuple<String>> allCases =
                redisTemplate.opsForZSet().rangeWithScores(HOT_CASES_KEY, 0, -1);

        if (allCases == null || allCases.isEmpty()) {
            log.info("감소시킬 랭킹이 없습니다.");
            return;
        }

        for (ZSetOperations.TypedTuple<String> tuple : allCases) {
            String caseId = tuple.getValue();
            double oldScore = tuple.getScore() != null ? tuple.getScore() : 0;

            // 2. 점수를 5% 감소시킴
            double newScore = oldScore * DECAY_FACTOR;

            if (newScore < MIN_SCORE_THRESHOLD) {
                // 3. 점수가 너무 낮아지면 랭킹에서 제거 (ZREM)
                redisTemplate.opsForZSet().remove(HOT_CASES_KEY, caseId);
            } else {
                // 4. 감소된 점수로 갱신 (ZADD - 덮어쓰기)
                redisTemplate.opsForZSet().add(HOT_CASES_KEY, caseId, newScore);
            }
        }
        log.info("총 {}개의 랭킹 점수 감소 처리 완료", allCases.size());
    }
}
