package com.demoday.ddangddangddang.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    // 1. Redis 접속 정보 설정 (Lettuce 사용)
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    // 2. RedisTemplate<String, String> 빈(Bean) 등록
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        // 2-1. 위에서 만든 접속 정보(ConnectionFactory) 설정
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // 2-2. Key 직렬화 방식을 String으로 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // 2-3. Value 직렬화 방식을 String으로 설정
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        // 2-4. Hash Key 직렬화 방식도 String으로 설정 (opsForHash 사용 대비)
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 2-5. Hash Value 직렬화 방식도 String으로 설정 (opsForHash 사용 대비)
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        // (선택) 모든 설정이 완료된 후, 설정값을 초기화
        // redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}