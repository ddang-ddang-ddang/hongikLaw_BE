package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.Like;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.DefenseRepository;
import com.demoday.ddangddangddang.repository.LikeRepository;
import com.demoday.ddangddangddang.repository.RebuttalRepository;
import com.demoday.ddangddangddang.repository.UserRepository; // 가정
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public boolean toggleLike(Long userId, Long contentId, ContentType contentType) {
        // (1) 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        // (2) 기존 좋아요가 있는지 확인
        Optional<Like> existingLike = likeRepository.findByUserAndContentIdAndContentType(user, contentId, contentType);

        if (existingLike.isPresent()) {
            // (3-A) 좋아요가 이미 존재하면 -> 삭제 (좋아요 취소 로직)
            Like like = existingLike.get();
            likeRepository.delete(like);

            // (4) 콘텐츠 타입에 따라 likesCount 1 감소
            if (contentType == ContentType.DEFENSE) {
                defenseRepository.decrementLikesCount(contentId);
            } else if (contentType == ContentType.REBUTTAL) {
                rebuttalRepository.decrementLikesCount(contentId);
            }
            return false; // "좋아요 취소됨"을 반환

        } else {
            // (3-B) 좋아요가 없으면 -> 생성 (좋아요 추가 로직)
            Like newLike = Like.builder()
                    .user(user)
                    .contentId(contentId)
                    .contentType(contentType)
                    .build();
            likeRepository.save(newLike);

            // (4) 콘텐츠 타입에 따라 likesCount 1 증가
            if (contentType == ContentType.DEFENSE) {
                defenseRepository.incrementLikesCount(contentId);
            } else if (contentType == ContentType.REBUTTAL) {
                rebuttalRepository.incrementLikesCount(contentId);
            }
            return true; // "좋아요 추가됨"을 반환
        }
    }
}
