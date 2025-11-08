package com.demoday.ddangddangddang;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration
public class TestConfig {
    @Bean
    public S3Client s3Client() {
        // S3Client 빈을 Mock 객체로 등록합니다.
        return Mockito.mock(S3Client.class);
    }
}
