package com.demoday.ddangddangddang.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisExampleService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate; // 문자열을 다룰 RedisTemplate 주입

    // Redis에 데이터 저장 (SET 명령어)
    public void saveData(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        System.out.println(key + "에 " + value + " 값을 저장했습니다.");
    }

    // Redis에서 데이터 조회 (GET 명령어)
    public String getData(String key) {
        String value = redisTemplate.opsForValue().get(key);
        System.out.println(key + "에서 " + value + " 값을 조회했습니다.");
        return value;
    }
}
