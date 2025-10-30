package com.demoday.ddangddangddang.global.config;

import com.demoday.ddangddangddang.global.exception.CustomAuthenticationEntryPoint;
import com.demoday.ddangddangddang.global.jwt.JwtAuthenticationFilter;
import com.demoday.ddangddangddang.global.jwt.JwtUtil;
import com.demoday.ddangddangddang.global.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 보호 비활성화
        http.csrf((csrf) -> csrf.disable());

        // 세션 관리 정책을 STATELESS로 설정
        http.sessionManagement((session) ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // STATELESS에서도 SecurityContext를 요청 간에 유지(request attribute 사용)
        http.securityContext((context) ->
                context.securityContextRepository(new RequestAttributeSecurityContextRepository())
        );

        // API 경로별 접근 권한 설정
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/api/v1/auth/**").permitAll() // /api/v1/auth/ 하위 경로는 모두 허용
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // Swagger 허용
                .requestMatchers("/").permitAll() // HomeController의 "/" 경로 허용
                .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
        );

        // 우리가 만든 JwtAuthenticationFilter를
        // UsernamePasswordAuthenticationFilter (스프링 시큐리티의 기본 로그인 필터) 앞에 추가
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        // 인증 예외(AuthenticationException) 처리 설정
        http.exceptionHandling((exceptions) -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
        );

        return http.build();
    }
}