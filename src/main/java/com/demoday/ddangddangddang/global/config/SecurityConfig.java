package com.demoday.ddangddangddang.global.config;

import com.demoday.ddangddangddang.global.exception.CustomAuthenticationEntryPoint;
import com.demoday.ddangddangddang.global.jwt.JwtAuthenticationFilter;
import com.demoday.ddangddangddang.global.jwt.JwtUtil;
import com.demoday.ddangddangddang.global.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

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

        http.cors((cors) -> cors.configurationSource(corsConfigurationSource()));
        
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
                .requestMatchers("/", "/health-check").permitAll()
                .requestMatchers("/*").permitAll() // HomeController의 "/" 경로 허용
                .requestMatchers(HttpMethod.GET,"/api/v1/cases/pending").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/cases/second").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/cases/search").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/cases/{caseId}").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/cases/{caseId}/defenses").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/finished").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/defenses/{defenseId}/rebuttals").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/cases/{caseId}/vote/result").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/cases/{caseId}/debate").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/final/judge/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/home/hot").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/final/adopt/{caseId}/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/final/judge/{caseId}/judgeStatus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/final/judge/{caseId}/history").permitAll()
                .requsetMatchers(HttpMethod.GET, "/api/v1/cases/{caseId}/judgment").permitAll()
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of(
                "http://localhost:5173",
                "https://ddangx3.site",
                "http://localhost:8080",
                "http://localhost:3000",
                "https://ddang-ddang-ddang-fe-8npo.vercel.app/",
                "https://api.ddangx3.site",
                "https://web.ddangx3.site",
                "https://www.ddangx3.site"
        ));
        config.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(java.util.List.of("Authorization", "Location"));

        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
