package com.demoday.ddangddangddang.service.auth;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.Rank;
import com.demoday.ddangddangddang.dto.auth.*;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode; // GeneralErrorCode 임포트
import com.demoday.ddangddangddang.global.exception.GeneralException; // GeneralException 임포트
import com.demoday.ddangddangddang.global.jwt.JwtUtil;
import com.demoday.ddangddangddang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Transactional
    public void signup(SignupRequestDto requestDto) {

        // 0. 이메일 '인증 완료' 상태 확인
        emailService.checkEmailVerified(requestDto.getEmail());

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
        Rank defaultRank = Rank.NEWBIE_BRAWLER;

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

        // 6. 회원가입 성공 시 Redis의 '인증 완료' 플래그 삭제
        emailService.deleteVerifiedEmailFlag(requestDto.getEmail());
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
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        // Refresh Token을 Redis에 저장
        // (Key: email, Value: refreshToken, Expiry: 7일)
        redisTemplate.opsForValue().set(
                user.getEmail(),
                refreshToken,
                7, // JwtUtil의 REFRESH_TOKEN_TIME (7일)과 일치시킴
                TimeUnit.DAYS
        );

        return new LoginResponseDto(accessToken, refreshToken);
    }

    /**
     * Access Token 재발급
     */
    @Transactional(readOnly = true)
    public AccessTokenResponseDto refreshAccessToken(TokenRefreshRequestDto requestDto) {
        String email = requestDto.getEmail();
        String refreshToken = requestDto.getRefreshToken();

        // 1. Redis에서 email로 저장된 Refresh Token 조회
        String storedToken = redisTemplate.opsForValue().get(email);

        // 2. Redis에 토큰이 없거나, 요청된 토큰과 일치하지 않으면 예외
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            // (GeneralErrorCode에 INVALID_TOKEN이 이미 있습니다)
            throw new GeneralException(GeneralErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
        }

        // 3. (선택 사항) JWT 라이브러리를 통해 토큰 유효성 재검증 (이미 만료되었는지 등)
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new GeneralException(GeneralErrorCode.INVALID_TOKEN, "만료되었거나 유효하지 않은 리프레시 토큰입니다.");
        }

        // 4. 유저 정보로 새 Access Token 생성
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));

        String newAccessToken = jwtUtil.createAccessToken(user.getEmail(), user.getId());

        return new AccessTokenResponseDto(newAccessToken);
    }
}