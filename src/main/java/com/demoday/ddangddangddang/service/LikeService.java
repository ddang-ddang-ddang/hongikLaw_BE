package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.event.UpdateJudgmentEvent;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.DefenseRepository;
import com.demoday.ddangddangddang.repository.LikeRepository;
import com.demoday.ddangddangddang.repository.RebuttalRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import com.demoday.ddangddangddang.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final DefenseRepository defenseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RankingService rankingService;

    /**
     * 변론 / 반론 좋아요 토글
     * @param userId 로그인된 유저 ID
     * @param contentId 대상 콘텐츠 (Defense or Rebuttal)
     * @param contentType DEFENSE / REBUTTAL
     * @return 현재 좋아요 상태 (true: 좋아요 됨 / false: 취소됨)
     */
    @Transactional
    public boolean toggleLike(Long userId, Long contentId, ContentType contentType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));

        Optional<Like> existingLike = likeRepository.findByUserAndContentIdAndContentType(user, contentId, contentType);

        Long caseIdToUpdate;
        boolean isLiked;

        if (existingLike.isPresent()) {
            // 👍 좋아요 취소
            likeRepository.delete(existingLike.get());

            if (contentType == ContentType.DEFENSE) {
                Defense defense = defenseRepository.findById(contentId)
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "변론을 찾을 수 없습니다."));
                defense.decrementLikesCount();
                caseIdToUpdate = defense.getACase().getId();
                rankingService.addCaseScore(caseIdToUpdate, -3.0);
            } else {
                Rebuttal rebuttal = rebuttalRepository.findById(contentId)
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "반론을 찾을 수 없습니다."));
                rebuttal.decrementLikesCount();
                caseIdToUpdate = rebuttal.getDefense().getACase().getId();
                rankingService.addCaseScore(caseIdToUpdate, -3.0);
            }

            isLiked = false;

        } else {
            // 💖 좋아요 추가
            Like newLike = Like.builder()
                    .user(user)
                    .contentId(contentId)
                    .contentType(contentType)
                    .build();
            likeRepository.save(newLike);

            if (contentType == ContentType.DEFENSE) {
                Defense defense = defenseRepository.findById(contentId)
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "변론을 찾을 수 없습니다."));
                defense.incrementLikesCount();
                caseIdToUpdate = defense.getACase().getId();
                rankingService.addCaseScore(caseIdToUpdate,3.0);
            } else {
                Rebuttal rebuttal = rebuttalRepository.findById(contentId)
                        .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "반론을 찾을 수 없습니다."));
                rebuttal.incrementLikesCount();
                caseIdToUpdate = rebuttal.getDefense().getACase().getId();
                rankingService.addCaseScore(caseIdToUpdate,3.0);
            }

            isLiked = true;
        }

        // ⚙️ AI 판결 업데이트 이벤트 (비동기)
        eventPublisher.publishEvent(new UpdateJudgmentEvent(caseIdToUpdate));

        return isLiked;
    }
}
