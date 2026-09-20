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

import java.time.LocalDateTime;
import java.util.Collections;

// Spring Security 로그인 처리 — DB에서 회원 조회
// Spring의 UserDetailsService 인터페이스를 구현하는 구현체
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserInfoRepository userInfoRepository;

    // 이메일로 회원 조회 후 Spring Security UserDetails 객체로 변환
    // Spring Security 가 로그인 시 자동으로 부름
    // UserDetailsService 인터페이스의 loadUserByUsername의 인터페이스 구현체(Override)
    // 그래서 class 이름을 CustomUserDetailsService.java 로 만들었음
    // [사전질문 Security 14] 로그인 시 비밀번호 비교는 어디서? → 여기 아님!
    //   이 메서드는 "해시를 담은 User를 반환"까지만. 실제 matches 비교는 Spring Security(DaoAuthenticationProvider)가 자동 수행
    @Override
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        UserInfo userInfo = userInfoRepository.findByUserEmail(userEmail) // userEmail로 DB를 조회
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일입니다: " + userEmail));

        boolean accountNonLocked = isAccountNonLocked(userInfo); // 잠금 판정

        // Spring Security가 이해하는 UserDetails 인터페이스를 User 구현체 객체로 변환해 반환
        // User에 해시 비밀번호, 권한, 잠금상태를 담아 반환
        return new User(
                userInfo.getUserEmail(),
                userInfo.getUserPwd(),       //  DB의 BCrypt 해시 (salt로 단방향 암호화) -> 비교 대상!
                true,                // enabled(계정 활성화 여부)
                true,                // accountNonExpired(계정이 만료 안 됨)
                true,                // credentialsNonExpired(비밀번호(자격증명)가 만료 안 됨)
                accountNonLocked,    // 잠금 상태 — false면 LockedException 자동 발생 , 5회 잠금 기능
                Collections.singletonList(new SimpleGrantedAuthority(userInfo.getUserRole()))  // 권한
        );
    }

    // lockedUntil이 미래 시각이면 잠금 상태
    // isAccountNonLocked() 는 lockedUntil(잠금 해제 시각)을
    // 현재 시각과 비교해서 null이거나 이미 지났으면 true(잠금 아님)을 반환
    private boolean isAccountNonLocked(UserInfo userInfo) {
        LocalDateTime lockedUntil = userInfo.getLockedUntil(); // 잠금 해제 예정 시각
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }
    // getLockedUntil() -> 회원 엔티티의 "잠금이 풀리는 시각"을 가져옴
    // 5회 실패로 잠기면 lockFor(30)이 여기에 현재시각 +30분을 넣어둠
    // 잠긴 적 없거나 풀렸으면 null

    // return 조건 -> 두 경우 중 하나면 "잠기지 않음(true)
    // lockedUntil == null -> 애초에 잠금 기록 없음 -> 정상
    // lockedUntil.isBefore(LocalDateTime.now() -> 잠금 해제 시각이 이미 지남 -> 잠금 풀림(정상)
}
