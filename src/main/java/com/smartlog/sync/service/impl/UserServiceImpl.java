package com.smartlog.sync.service.impl;

import com.smartlog.sync.dto.SignupDto;
import com.smartlog.sync.dto.UserInfoDto;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.repository.UserInfoRepository;
import com.smartlog.sync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// 회원 관련 비즈니스 로직 구현체
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_LOGIN_FAIL = 5;       // 잠금 임계값
    private static final int LOCK_MINUTES = 30;        // 잠금 유지 시간(분)

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void signup(SignupDto dto) {
        if (userInfoRepository.existsByUserEmail(dto.userEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }
        UserInfo userInfo = UserInfo.builder()
                .userEmail(dto.userEmail())
                .userPwd(passwordEncoder.encode(dto.userPwd()))
                .userName(dto.userName())
                .orgName(dto.orgName())
                .userRole(dto.userRoleSafe())   // 폼 선택값(화이트리스트 검증) — 미지정 시 ROLE_USER
                .build();
        userInfoRepository.save(userInfo);
    }

    @Override
    public boolean isEmailDuplicate(String email) {
        return userInfoRepository.existsByUserEmail(email);
    }

    @Override
    public String findEmail(String userName, String orgName) {
        UserInfo user = userInfoRepository.findByUserNameAndOrgName(userName, orgName)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정이 없습니다"));
        return maskEmail(user.getUserEmail());
    }

    @Override
    public void resetPassword(String userEmail, String newPassword) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        user.changePassword(passwordEncoder.encode(newPassword));
        userInfoRepository.save(user);
    }

    @Override
    public void updateProfile(String userEmail, String userName, String orgName) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        user.updateProfile(userName, orgName);
        userInfoRepository.save(user);
    }

    @Override
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        if (!passwordEncoder.matches(currentPassword, user.getUserPwd())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        userInfoRepository.save(user);
    }

    @Override
    public UserInfoDto getUserDtoByEmail(String userEmail) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail).orElse(null);
        return UserInfoDto.from(user);
    }

    @Override
    public UserInfo getEntityByEmail(String userEmail) {
        return userInfoRepository.findByUserEmail(userEmail).orElse(null);
    }

    @Override
    public void recordLoginFailure(String userEmail) {
        // 존재하지 않는 이메일은 조용히 무시 (User Enumeration 방지)
        UserInfo user = userInfoRepository.findByUserEmail(userEmail).orElse(null);
        if (user == null) return;

        user.incrementFailCount();
        if (user.getFailCount() >= MAX_LOGIN_FAIL) {
            user.lockFor(LOCK_MINUTES);
        }
        userInfoRepository.save(user);
    }

    @Override
    public void resetLoginFailures(String userEmail) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail).orElse(null);
        if (user == null) return;
        user.resetLoginFailures();
        userInfoRepository.save(user);
    }

    // 이메일 마스킹 (ho**@smartlog.kr)
    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return email;
        return email.substring(0, 2) + "**" + email.substring(atIdx);
    }
}
