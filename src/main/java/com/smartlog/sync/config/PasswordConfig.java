package com.smartlog.sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// PasswordEncoder 빈 분리
// — SecurityConfig에 두면 UserService→PasswordEncoder→SecurityConfig→LoginFailureHandler→UserService 순환 발생
// — 빈 정의를 별도 Config로 빼서 SecurityConfig에 대한 간접 의존을 끊음
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
