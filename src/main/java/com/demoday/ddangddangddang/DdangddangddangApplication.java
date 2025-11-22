package com.demoday.ddangddangddang;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@EnableAsync
@EnableJpaAuditing
@EnableJpaRepositories
@SpringBootApplication
public class DdangddangddangApplication {

	@PostConstruct
	public void started() {
		// 어플리케이션 실행 시, 전역 시간대를 한국으로 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(DdangddangddangApplication.class, args);
	}

}
