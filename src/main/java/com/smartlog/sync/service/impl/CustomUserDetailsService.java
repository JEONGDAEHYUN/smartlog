package com.smartlog.sync.service.impl;

import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

// Spring Security 로그인 처리 — DB에서 회원 조회
// Spring의 UserDetailsService 인터페이스를 구현하는 구현체
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserInfoRepository userInfoRepository;

    // 이메일로 회원 조회 후 Spring Security UserDetails 객체로 변환
    @Override
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        UserInfo userInfo = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일입니다: " + userEmail));

        return new User(
                userInfo.getUserEmail(),
                userInfo.getUserPwd(),
                Collections.singletonList(new SimpleGrantedAuthority(userInfo.getUserRole()))
        );
    }
}
