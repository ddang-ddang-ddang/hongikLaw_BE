package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.Rank;
import com.demoday.ddangddangddang.dto.request.LoginRequestDto;
import com.demoday.ddangddangddang.dto.request.SignupRequestDto;
import com.demoday.ddangddangddang.dto.response.LoginResponseDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode; // GeneralErrorCode 임포트
import com.demoday.ddangddangddang.global.exception.GeneralException; // GeneralException 임포트
import com.demoday.ddangddangddang.global.jwt.JwtUtil;
import com.demoday.ddangddangddang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void signup(SignupRequestDto requestDto) {
        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            // GeneralException 사용하도록 수정
            throw new GeneralException(GeneralErrorCode.DUPLICATE_LOGINID, "이미 존재하는 이메일입니다.");
        }
        // 2. 닉네임 중복 확인
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            // GeneralException 사용하도록 수정 (적절한 에러 코드가 필요합니다. DUPLICATE_LOGINID를 임시로 사용)
            throw new GeneralException(GeneralErrorCode.DUPLICATE_LOGINID, "이미 존재하는 닉네임입니다.");
        }

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 4. 기본 랭크 설정 (Enum 직접 사용)
        Rank defaultRank = Rank.a;

        // 5. 유저 객체 생성 및 저장
        User user = User.builder()
                .email(requestDto.getEmail())
                .nickname(requestDto.getNickname())
                .password(encodedPassword)
                .rank(defaultRank)
                .exp(0L)
                .totalPoints(0)
                .winCnt(0)
                .loseCnt(0)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_LOGIN));

        // 2. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new GeneralException(GeneralErrorCode.INVALID_LOGIN);
        }

        // 3. 토큰 생성
        String accessToken = jwtUtil.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtUtil.createRefreshToken();

        // (추후 Refresh Token은 DB나 Redis에 저장하는 로직 필요)

        return new LoginResponseDto(accessToken, refreshToken);
    }
}