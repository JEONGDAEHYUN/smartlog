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
// [사전질문 Security 1] Spring Security 왜 적용? → 인증·인가·잠금·RememberMe·CSRF를 표준 구현하려고
// [사전질문 Security 5] URL 접근 권한 설정 파일 → 이 SecurityConfig.filterChain 안
@Configuration     // 이 클래스가 스프링 "설정 클래스"임을 선언 (빈 정의를 담음)
@EnableWebSecurity // 스프링 시큐리티 웹 보안 기능을 켜는 스위치 (필터 체인 활성화)
@RequiredArgsConstructor // final 필드를 받는 생성자를 Lobok이 자동 생성 -> 생성자 주입
public class SecurityConfig {

    // @RequiredArgsConstructor 로 final 필드에 생성자가 주입됨
    private final LoginFailureHandler loginFailureHandler; // 로그인 실패 핸들러 주입
    private final LoginSuccessHandler loginSuccessHandler; //  로그인 성공 핸들러 주입
    private final UserDetailsService userDetailsService;   // CustomUserDetailsService 가 주입됨
                                                            // CustomUserDetailService는 userDetailsService의 인터페이스 구현체

    // SessionRegistry, PersistentTokenRepository 는 메서드 파라미터로 주입받음 (self-injection 회피)
    // Bean: 이 메서드가 반환하는 SecurityFilterChain 객체를 스프링 컨테이너가 관리하는 빈(Bean)으로 등록
    // 스프링 시큐리티는 이 필터 체인을 통해 들어오는 모든 HTTP 요청의 보안을 처리
    // [사전질문 Security 6] SecurityConfig 핵심 설정 = 이 filterChain 한 메서드(csrf·인가·세션·login·logout·rememberMe)
    // [사전질문 Security 7] SecurityFilterChain 역할 = 모든 요청이 통과하는 보안 필터 묶음 (http.build()로 생성)
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,                             // 보안 설정을 구성할 수 있도록 지원하는 핵심 빌더 클래스
            SessionRegistry sessionRegistry,               // 현재 활성화된 세션들을 추적하고 관리하는 컴포넌트
            // 자동 로그인(Remember-Me)정보를 데이터베이스(persistent_logins 테이블)에 영구 저장하기 위한 저장소 인터페이스
            PersistentTokenRepository tokenRepository) {
        http
            // [사전질문 Security 15] CSRF 비활성화 이유 → 전체 X, fetch JSON API(/schedule/api/**)만 예외. 폼 POST는 보호 유지
            // CSRF — fetch 기반 JSON API 경로만 예외 처리 (폼 POST 는 CSRF 보호 유지)
                // .csrf(...): CSRF 방어 기능을 설정, Spring Security는 기본적으로 로그인여부와 무관하게
                // 모든 상태 변경 요청(POST, PUT, DELETE 등)에 대해 CSRF 토큰을 요구
            .csrf(csrf -> csrf

                // /schedule/api/ 로 시작하는 모든 경로(예: 비동기 AJAX/fetch 요청이 발생하는 API 경로)에 대해서는
                // CSRF 토큰 검증을 제외(무시)하겠단는 설정
                // 이유: 브라우저 폼 전송과 달리, 자바스크립트(fetch, axios)로 JSON 데이터를 주고받는 Restful API 영역은
                //      CSRF 방어를 끄거나 별도의 헤더 처리를 하는 경우가 많기 때문임
                //      그 외의 일반 페이지 폼 POST 요청은 여전히 보호받음
                .ignoringRequestMatchers("/schedule/api/**")
            )
            // 접근 권한 설정 , 인가 규칙
            // 요청에 대한 인증/인가 규칙을 정의
            // [사전질문 Security 2] 인가(Authorization) 부분 = 이 authorizeHttpRequests (인증=아래 formLogin)
            // [사전질문 Security 8] requestMatchers 역할 = URL 패턴 골라 규칙 적용
            // [사전질문 Security 9] permitAll(누구나) vs authenticated(로그인 필수) 차이
            .authorizeHttpRequests(auth -> auth
                    // [사전질문 Security 3] 로그인 없이 접근 가능한 URL = 아래 permitAll 목록
                    // 나열된 경로들에 대해서는 로그인하지 않은 익명의 사용자도 접근할 수 있도록 전면 허용함
                .requestMatchers("/", "/login", "/signup", "/signup/verify", "/find-id", "/find-pw", "/find-pw/**", "/css/**", "/js/**", "/images/**").permitAll()
                    // 관리자 전용 URL — ROLE_ADMIN 만 접근 가능
                .requestMatchers("/admin/**").hasRole("ADMIN")
                    // [사전질문 Security 4] 로그인해야만 접근 가능한 URL = permitAll 제외 전부(anyRequest)
                    // 위에서 명시한 permitAll() 경로들을 제외한 모든 요청(anyRequest)은 반드시 로그인(인증)을 거쳐야만 접근할 수 있도록 통제함
                .anyRequest().authenticated()
            )
            // 세션 관리 — SessionRegistry로 추적 (비밀번호 변경 시 강제 만료에 사용)
            // 세션 생성 정책 및 동시성 제어 등의 세션 관리 규칙을 정의함
            .sessionManagement(session -> session

                    // 한 사용자가 동시에 가질 수 있는 세션의 개수를 제한하는 함수
                    // 값을 -1로 설정하면 세션 개수 제한하지 않음
                .maximumSessions(-1)                       // 동시 세션 무제한 (만료 기능만 활용)

                    // 주입받은 SessionRegistry 를 등록하여 스프링 시큐리티가 현재 로그인한 유저들의 세션 메타데이터를
                    // 메모리에 기록 추적하게 만듬
                    // 사용자가 비밀번호를 변경했을 때, 이전 기기들에서 로그인된 세션들을 강제로 찾아내 파기(seeeionInformation.expireNow()) 하기 위한 기반 작업
                .sessionRegistry(sessionRegistry)

                    //다른 곳에 의해 세션이 강제로 만료(Expired)된 사용자가 페이지를 요청하면
                    // 자동으로 이 URL로 리다이렉트 시킨다.
                    // 쿼리 스트링(?expired=true)을 통해 로그인 페이지에서 세션 만료 되었음 같은 안내 문구 띄우게 설계
                .expiredUrl("/login?expired=true")         // 만료된 세션 접근 시 한국어 안내 페이지로
            )
            // 질문 Security 2 인증(Authentication) 부분 = 이 formLogin (로그인으로 신원 확인)
            // 로그인 설정
            // 사용자가 아이디/비밀번호를 입력하는 폼 기반 로그인 기능을 활성화하고 설정함
            .formLogin(form -> form

                    // 인증되지 않은 사용자가 보호된 페이지에 접근했을때 이동시킬 커스텀 로그인 페이지 경로를 지정
                .loginPage("/login")

                    // 로그인이 성공했을 때 실행할 커스텀 로직 클래스를 지정한다.
                    // 로그인 성공시 successHandler 가 resetLoginFailures를 호출해서 failCount를 0으로 리셋
                    // 잠금(lockedUntil)도 해제
                .successHandler(loginSuccessHandler)    // 성공 시 실패 횟수 초기화

                    // 로그인 실패 시 실행할 커스텀 처리 클래스를 지정하는 줄
                    // 실제 처리는 LoginFailureHandler.java
                .failureHandler(loginFailureHandler)    // 실패 시 횟수 카운트, 5회 잠금

                    // 로그인 관련 URL은 누구나 접근 허용
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
                .tokenRepository(tokenRepository)       // 토큰을 DB에 저장하는 저장소
                .userDetailsService(userDetailsService) // 자동 로그인 시 사용자 재조회
                .tokenValiditySeconds(60 * 60 * 24)        // 1일
                .rememberMeParameter("remember-me")        // 폼 체크박스 name 과 일치
                .rememberMeCookieName("remember-me")       // 발급되는 쿠키 이름
                .key("smartlog-sync-remember-me-key")      // [검토 필요] 운영 시 환경변수(${REMEMBER_ME_KEY})로 분리
            );

        // 체인 객체를 만들어 반환
        return http.build();
    }

    // RememberMe 토큰 저장소 — MariaDB persistent_logins 테이블 사용 -> 이곳에 저장하기위해 정의 하는 빈
    // 스키마는 src/main/resources/sql/persistent_logins.sql 에 정의 (수동 실행)
    @Bean
    @SuppressWarnings("removal")  // setDataSource — Spring 7.0에서 JdbcDaoSupport deprecated, 실동작 정상
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {

        // JDBC로 DB에 토큰 저장하는 구현체
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();

        // 어느 DB에 저장할지 연결 (내 MariaDB)
        repository.setDataSource(dataSource);

        // persistent_logins 테이블 사용, 값 리턴
        return repository;
    }

    // 활성 세션 추적기 — 사용자별 세션 강제 만료에 사용 쉽게 설명하면 누가 어떤 세션을 갖고 있는지 메모리에 기록함
    @Bean
    public SessionRegistry sessionRegistry() {

        // 비밀번호 변경 시 사용자(누가 어떤 세션을 갖고 있는지 기록된 것에서) 세션 강제 만료에 사용
        return new SessionRegistryImpl();
    }

    // 이 빈은 세션 생성/소멸 이벤트를 SessionRegistry에 전파 (SessionRegistryImpl 동작에 필수)
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // SessionRegistry가 세션을 추적하려면, 세션이 만들어지고 사라지는 이벤트를 누군가 알려줘야 하는데
    // 그게 HttpSessionEventPublisher 이다.
}
