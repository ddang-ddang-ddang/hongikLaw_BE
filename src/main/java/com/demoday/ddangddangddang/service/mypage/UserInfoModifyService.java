package com.demoday.ddangddangddang.service.mypage;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.UserRepository;
import com.demoday.ddangddangddang.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInfoModifyService {
    //유저 정보 조회, 수정
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    //마이페이지 사용자 정보
    @Transactional(readOnly = true)
    public ApiResponse<User> getUsers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        User getUser = User.builder()
                .nickname(user.getNickname())
                .nickname(user.getNickname())
                .profileImageurl(user.getProfileImageurl())
                .email(user.getEmail())
                .rank(user.getRank())
                .build();
        return ApiResponse.onSuccess("유저 정보 조회 성공",getUser);
    }

}
