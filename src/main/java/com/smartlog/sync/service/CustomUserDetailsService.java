package com.smartlog.sync.service;

import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

// Spring Security 로그인 처리 — DB에서 회원 조회
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
