package com.demoday.ddangddangddang.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    /*
    @Bean
    public OpenAiService openAiService() {
        // 'com.theokanning.openai...' (0.18.2) 라이브러리의 Bean 생성
        return new OpenAiService(openAiApiKey, Duration.ofSeconds(60));
    }
     */

    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(openAiApiKey)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 3. Java 8의 시간 타입(LocalDateTime 등)을 처리하도록 모듈 등록
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}