package com.demoday.ddangddangddang.service.mypage;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.mypage.UserUpdateRequestDto;
import com.demoday.ddangddangddang.dto.mypage.UserUpdateResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.UserRepository;
import com.demoday.ddangddangddang.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
                .profileImageUrl(user.getProfileImageUrl())
                .email(user.getEmail())
                .rank(user.getRank())
                .build();
        return ApiResponse.onSuccess("유저 정보 조회 성공",getUser);
    }

    //마이페이지 정보 수정
    @Transactional
    public ApiResponse<UserUpdateResponseDto> modifyUser(Long userId, UserUpdateRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        user.updateMypageInfo(requestDto);

        UserUpdateResponseDto responseDto = UserUpdateResponseDto.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImage(user.getProfileImageUrl())
                .build();

        return ApiResponse.onSuccess("유저 정보 업데이트 성공",responseDto);
    }

    //프로필 사진 등록
    @Transactional
    public ApiResponse<String> updateUserProfileImage(Long userId, MultipartFile profileImage)  {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        try {
            // 기존 프로필 이미지가 있는 경우 S3에서 삭제
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                s3Uploader.deleteFile(user.getProfileImageUrl());
            }

            // 새 프로필 이미지를 S3에 업로드
            String imageUrl = s3Uploader.upload(profileImage, "profile"); // "profile"은 S3 버킷 내의 폴더명

            // 사용자 정보에 새 이미지 URL 저장
            user.updateProfileImageUrl(imageUrl);

            return ApiResponse.onSuccess("프로필 사진 변경 완료 : " + imageUrl); // 새 이미지 URL 반환

        } catch (IOException e) {
            // 파일 업로드 실패 시 예외 처리
            throw new RuntimeException("프로필 이미지 업로드에 실패했습니다.", e);
        }
    }

}
