package com.demoday.ddangddangddang.listener;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.UserAchievement;
import com.demoday.ddangddangddang.domain.enums.CaseResult;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.domain.enums.achieve.AchieveEnum;
import com.demoday.ddangddangddang.domain.event.CaseCreatedEvent;
import com.demoday.ddangddangddang.domain.event.CaseParticipationEvent;
import com.demoday.ddangddangddang.domain.event.PostCreatedEvent;
import com.demoday.ddangddangddang.domain.event.WinEvent;
import com.demoday.ddangddangddang.repository.CaseParticipationRepository;
import com.demoday.ddangddangddang.repository.CaseRepository;
import com.demoday.ddangddangddang.repository.RebuttalRepository;
import com.demoday.ddangddangddang.repository.UserAchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementEventListener {

    private final UserAchievementRepository userAchievementRepository;
    private final CaseRepository caseRepository;
    private final CaseParticipationRepository caseParticipationRepository;
    private final RebuttalRepository rebuttalRepository;

    /**
     * 사건 생성 이벤트를 수신하여 처리
     */
    @EventListener
    @Transactional
    @Async
    public void handleCaseCreated(CaseCreatedEvent event) {
        User user = event.getUser();

        // 1. 현재 유저의 사건 생성 횟수 조회 (DB 부하를 줄이려면 count 쿼리 최적화 필요)
        Integer caseCount = caseParticipationRepository.countByUser(user);

        // 2. 조건 체크 및 업적 지급
        if (caseCount == 1) {
            giveAchievement(user, AchieveEnum.FIRST_CASE);
        } else if (caseCount == 10) {
            giveAchievement(user, AchieveEnum.CASE_10);
        }
    }

    @EventListener
    @Transactional
    @Async
    public void handleCaseParticipation(CaseParticipationEvent event) {
        User user = event.getUser();

        // 1. 현재 유저의 사건 생성 횟수 조회 (DB 부하를 줄이려면 count 쿼리 최적화 필요)
        Integer caseCount = caseParticipationRepository.countByUser(user);

        // 2. 조건 체크 및 업적 지급
        if (caseCount == 1) {
            giveAchievement(user, AchieveEnum.FIRST_VS);
        }
    }

    @EventListener
    @Transactional
    @Async
    public void handleWin(WinEvent event) {
        User user = event.getUser();

        // 1. 현재 유저의 승리 횟수 조회
        Integer winCountCase = caseParticipationRepository.countByUserAndResult(user, CaseResult.WIN);
        Integer winCountDefense = 0;
        Integer winCountRebuttal = 0;
        Integer winCountTotal = winCountCase + winCountDefense + winCountRebuttal;

        // 2. 조건 체크 및 업적 지급
        if (winCountTotal == 1) {
            giveAchievement(user, AchieveEnum.FIRST_WIN);
        }
        else if (winCountTotal == 10) {
            giveAchievement(user, AchieveEnum.WIN_10);
        }
    }

    @EventListener
    @Transactional
    @Async
    public void handlePostCreated(PostCreatedEvent event) {
        User user = event.getUser();
        ContentType contentType = event.getContentType();

        Integer defenseCount = 0;
        Integer rebuttalCount = 0;

        // 1. 현재 유저의 사건 생성 횟수 조회 (DB 부하를 줄이려면 count 쿼리 최적화 필요)
        if(contentType == ContentType.DEFENSE){
            defenseCount = caseParticipationRepository.countByUser(user);
        }
        else {
            rebuttalCount = rebuttalRepository.countByUser(user);
        }

        // 2. 조건 체크 및 업적 지급
        if (defenseCount == 1) {
            giveAchievement(user, AchieveEnum.FIRST_DEFENSE);
        } else if (defenseCount == 10) {
            giveAchievement(user,AchieveEnum.DEFENSE_10);
        } else if (defenseCount == 50) {
            giveAchievement(user,AchieveEnum.DEFENSE_50);
        }

        if (rebuttalCount == 1) {
            giveAchievement(user, AchieveEnum.FIRST_REBUTTAL);
        } else if (rebuttalCount == 50) {
            giveAchievement(user, AchieveEnum.REBUTTAL_50);
        }
    }

    /**
     * 업적 지급 공통 메서드 (중복 지급 방지 로직 포함)
     */
    private void giveAchievement(User user, AchieveEnum achieveEnum) {
        // 이미 가지고 있는지 체크 (중복 방지)
        boolean alreadyHas = userAchievementRepository.existsByUserAndAchievement(user, achieveEnum);

        if (!alreadyHas) {
            UserAchievement newAchievement = UserAchievement.builder()
                    .user(user)
                    .achievement(achieveEnum)
                    .earnedAt(LocalDateTime.now())
                    .build();

            userAchievementRepository.save(newAchievement);
            log.info("업적 달성! 사용자: {}, 업적: {}", user.getId(), achieveEnum.getName());
        }
    }
}