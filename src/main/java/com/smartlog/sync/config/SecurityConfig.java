package com.smartlog.sync.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Spring Security 설정
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginFailureHandler loginFailureHandler;
    private final LoginSuccessHandler loginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/signup", "/signup/verify", "/find-id", "/find-pw", "/find-pw/**", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
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
                .permitAll()
            );

        return http.build();
    }

    // 비밀번호 암호화 (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
