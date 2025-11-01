package com.demoday.ddangddangddang.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Bean
    public OpenAiService openAiService() {
        // 'openai-java' 라이브러리의 핵심 서비스 객체
        // 60초 타임아웃 설정
        return new OpenAiService(openAiApiKey, Duration.ofSeconds(60));
    }

    @Bean
    public ObjectMapper objectMapper() {
        // AI가 반환하는 JSON을 파싱하기 위한 ObjectMapper
        return new ObjectMapper();
    }
}