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

    // 이 메서드 반환값을(비밀번호) 스프링이 관리하는 빈으로 등록
    // [사전질문 Security 12] PasswordEncoder 역할 = encode(암호화)/matches(검증) 제공
    // [사전질문 Security 13] BCrypt 복호화 가능? → 불가(단방향), salt 포함 매번 다른 해시 → matches로만 비교
    @Bean
    public PasswordEncoder passwordEncoder() { // PasswordEncoder 타입 빈을  만드는 메서드
        return new BCryptPasswordEncoder();    // BCrypt는 hash-256과 다름 자동으로 salt 해서 매번 다른 해시값 나옴
    }
}
