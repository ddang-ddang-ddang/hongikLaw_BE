package com.demoday.ddangddangddang.service.auth;

import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private static final String AUTH_CODE_PREFIX = "AuthCode:";
    private static final long AUTH_CODE_EXPIRATION_MINUTES = 5; // 5분

    /**
     * 인증번호 이메일 발송
     * (실제 발송은 주석 처리, 로그로 대체)
     */
    public void sendVerificationCode(String email) {
        String authCode = createAuthCode();

         // 1. (Mock) 이메일 발송

         try {
             SimpleMailMessage message = new SimpleMailMessage();
             message.setTo(email);
             message.setSubject("[땅땅땅] 회원가입 인증번호 안내");
             message.setText("인증번호는 [" + authCode + "] 입니다.");
             mailSender.send(message);
         } catch (MailException e) {
             log.error("이메일 발송 실패: {}", email, e);
             throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다.");
         }

        // (주의) 실제 발송 대신 로그 출력
        log.info("[EmailService] 인증번호 발송 요청: {} / 인증번호: {}", email, authCode);

        // 2. Redis에 인증번호 저장 (Key: "AuthCode:email@example.com", Value: "123456")
        String redisKey = AUTH_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(
                redisKey,
                authCode,
                AUTH_CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
    }

    /**
     * 인증번호 검증
     */
    public boolean verifyCode(String email, String code) {
        String redisKey = AUTH_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            // 인증번호가 만료되었거나 존재하지 않음
            throw new GeneralException(GeneralErrorCode.INVALID_AUTH_CODE, "인증번호가 만료되었거나 존재하지 않습니다.");
        }

        if (!storedCode.equals(code)) {
            // 인증번호 불일치
            throw new GeneralException(GeneralErrorCode.INVALID_AUTH_CODE, "인증번호가 일치하지 않습니다.");
        }

        // 검증 성공
        return true;
    }

    /**
     * 인증 완료 후 Redis에서 코드 삭제
     */
    public void deleteCode(String email) {
        String redisKey = AUTH_CODE_PREFIX + email;
        redisTemplate.delete(redisKey);
    }

    /**
     * 6자리 랜덤 인증번호 생성
     */
    private String createAuthCode() {
        Random random = new Random();
        int number = random.nextInt(899999) + 100000; // 100000 ~ 999999
        return String.valueOf(number);
    }
}