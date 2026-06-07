package com.smartlog.sync.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import javax.sql.DataSource;

// Spring Security 설정
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginFailureHandler loginFailureHandler;
    private final LoginSuccessHandler loginSuccessHandler;
    private final UserDetailsService userDetailsService;   // CustomUserDetailsService 자동 주입

    // SessionRegistry, PersistentTokenRepository 는 메서드 파라미터로 주입받음 (self-injection 회피)
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            SessionRegistry sessionRegistry,
            PersistentTokenRepository tokenRepository) {
        http
            // CSRF — fetch 기반 JSON API 경로만 예외 처리 (폼 POST 는 CSRF 보호 유지)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/schedule/api/**")
            )
            // 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/signup", "/signup/verify", "/find-id", "/find-pw", "/find-pw/**", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            // 세션 관리 — SessionRegistry로 추적 (비밀번호 변경 시 강제 만료에 사용)
            .sessionManagement(session -> session
                .maximumSessions(-1)                       // 동시 세션 무제한 (만료 기능만 활용)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/login?expired=true")         // 만료된 세션 접근 시 한국어 안내 페이지로
            )
            // 로그인 설정
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginSuccessHandler)    // 성공 시 실패 횟수 초기화
                .failureHandler(loginFailureHandler)    // 실패 시 횟수 카운트, 5회 잠금
                .permitAll()
            )
            // 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true") // 로그아웃 후 로그인 페이지로
                .deleteCookies("remember-me", "SYNC_SESSION") // 로그아웃 시 RememberMe 쿠키도 제거
                .permitAll()
            )
            // 로그인 상태 유지 (Persistent Token 방식 — persistent_logins 테이블에 토큰 저장 + 회전)
            .rememberMe(rm -> rm
                .tokenRepository(tokenRepository)
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(60 * 60 * 24)        // 1일
                .rememberMeParameter("remember-me")        // 폼 체크박스 name 과 일치
                .rememberMeCookieName("remember-me")       // 발급되는 쿠키 이름
                .key("smartlog-sync-remember-me-key")      // [검토 필요] 운영 시 환경변수(${REMEMBER_ME_KEY})로 분리
            );

        return http.build();
    }

    // RememberMe 토큰 저장소 — MariaDB persistent_logins 테이블 사용
    // 스키마는 src/main/resources/sql/persistent_logins.sql 에 정의 (수동 실행)
    @Bean
    @SuppressWarnings("removal")  // setDataSource — Spring 7.0에서 JdbcDaoSupport deprecated, 실동작 정상
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        // setCreateTableOnStartup(true) 사용하지 않음 — 두 번째 기동부터 "Table already exists" 에러 발생
        return repository;
    }

    // 활성 세션 추적기 — 사용자별 세션 강제 만료에 사용
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // 세션 생성/소멸 이벤트를 SessionRegistry에 전파 (SessionRegistryImpl 동작에 필수)
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
