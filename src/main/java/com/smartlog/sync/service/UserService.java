package com.smartlog.sync.service;

import com.smartlog.sync.dto.SignupDto;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// 회원 관련 비즈니스 로직
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 처리
    public void signup(SignupDto dto) {
        // 이메일 중복 확인
        if (userInfoRepository.existsByUserEmail(dto.getUserEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        // Entity 생성 및 저장
        UserInfo userInfo = UserInfo.builder()
                .userEmail(dto.getUserEmail())
                .userPwd(passwordEncoder.encode(dto.getUserPwd())) // BCrypt 암호화
                .userName(dto.getUserName())
                .orgName(dto.getOrgName())
                .userRole("ROLE_USER") // 기본 권한
                .build();

        userInfoRepository.save(userInfo);
    }

    // 이메일 중복 확인
    public boolean isEmailDuplicate(String email) {
        return userInfoRepository.existsByUserEmail(email);
    }

    // 아이디(이메일) 찾기 — 이름 + 조직명으로 조회, 마스킹 처리
    public String findEmail(String userName, String orgName) {
        UserInfo user = userInfoRepository.findByUserNameAndOrgName(userName, orgName)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정이 없습니다"));
        return maskEmail(user.getUserEmail());
    }

    // 비밀번호 재설정
    public void resetPassword(String userEmail, String newPassword) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        user.setUserPwd(passwordEncoder.encode(newPassword));
        userInfoRepository.save(user);
    }

    // 개인정보 수정 (이름, 조직명)
    public void updateProfile(String userEmail, String userName, String orgName) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        user.setUserName(userName);
        user.setOrgName(orgName);
        userInfoRepository.save(user);
    }

    // 비밀번호 변경 (현재 비밀번호 검증 포함)
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        if (!passwordEncoder.matches(currentPassword, user.getUserPwd())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
        }
        user.setUserPwd(passwordEncoder.encode(newPassword));
        userInfoRepository.save(user);
    }

    // 이메일 마스킹 (ho**@smartlog.kr)
    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return email;
        return email.substring(0, 2) + "**" + email.substring(atIdx);
    }
}
