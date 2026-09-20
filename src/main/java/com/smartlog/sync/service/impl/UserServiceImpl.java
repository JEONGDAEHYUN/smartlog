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
// 역할: Controller 와 Repository(DB) 사이에서 회원가입·계정찾기·프로필/비번 수정·로그인 실패잠금 같은
//      "회원 도메인 비즈니스 로직"을 모아 처리한다. UserService 인터페이스의 구현체.

// @Service : 이 클래스를 "서비스 계층 빈"으로 등록 (Controller 가 주입받아 사용)
@Service
// @RequiredArgsConstructor : final 필드(아래 2개)를 받는 생성자를 Lombok이 자동 생성 (생성자 주입)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_LOGIN_FAIL = 5;       // 잠금 임계값 (연속 5회 실패 시 잠금)
    private static final int LOCK_MINUTES = 30;        // 잠금 유지 시간(분) 상수로 선언

    private final UserInfoRepository userInfoRepository; // DB 접근(회원 CRUD)
    private final PasswordEncoder passwordEncoder;       // 비밀번호 암호화/검증(BCrypt)

    // ── 회원가입 ──
    // [사전질문 공통 6] Service가 처리하는 비즈니스 로직 예 = 이 signup (중복확인→암호화→검증→저장)
    // 흐름: 이메일 중복 확인 → 비밀번호 암호화 → 엔티티 빌드 → DB 저장(INSERT)
    @Override
    public void signup(SignupDto dto) {
        // 이미 같은 이메일이 있으면 가입 불가 (예외 던짐) → 동시가입/중복가입 방지
        if (userInfoRepository.existsByUserEmail(dto.userEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }
        // 빌더 패턴으로 UserInfo 엔티티 생성
        UserInfo userInfo = UserInfo.builder()
                .userEmail(dto.userEmail())
                // [사전질문 Security 11] 비밀번호 평문 저장? → 아니요. encode()로 BCrypt 해싱한 값만 저장
                .userPwd(passwordEncoder.encode(dto.userPwd())) // 비밀번호는 해당함수로 BCrypt 해싱한 값만 저장한다 ,db 유출되도 복호화 불가 단방향
                .userName(dto.userName())
                .orgName(dto.orgName())
                .userRole(dto.userRoleSafe())   // 폼 선택값 , 권한
                .build();
        // [사전질문 JPA 9] save()는 insert/update 중 무엇? → 여긴 PK 없는 새 객체라 INSERT
        // 만든 엔티티를 DB에 저장 (PK 없는 새 객체 → INSERT)
        userInfoRepository.save(userInfo);
    }

    // ── 이메일 중복 확인 ── (회원가입 화면에서 사용)
    // 해당 이메일이 DB에 이미 있으면 true 반환
    @Override
    public boolean isEmailDuplicate(String email) {
        return userInfoRepository.existsByUserEmail(email);
    }

    // ── 아이디(이메일) 찾기 ──
    // 이름+조직명으로 회원을 찾아, 이메일을 마스킹(ho**@...)해서 반환
    @Override
    public String findEmail(String userName, String orgName) {
        UserInfo user = userInfoRepository.findByUserNameAndOrgName(userName, orgName)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정이 없습니다")); // 없으면 예외
        return maskEmail(user.getUserEmail()); // 전체 노출 방지 위해 마스킹 후 반환
    }


    // ── 비밀번호 재설정 ── (비밀번호 찾기 흐름)
    // 이메일로 회원 조회 → 새 비번을 BCrypt 암호화해서 교체 → 저장(UPDATE)
    @Override
    public void resetPassword(String userEmail, String newPassword) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        user.changePassword(passwordEncoder.encode(newPassword)); // 암호화된 값으로 교체(도메인 메서드)
        userInfoRepository.save(user); // 기존 객체 수정 → UPDATE
    }

    // ── 프로필 수정 (마이페이지) ──
    // 이름·조직 수정 + 이메일(로그인 ID) 변경까지. 이메일이 실제로 바뀌면 true 반환(재로그인 유도용)
    @Override
    public boolean updateProfile(String currentEmail, String newEmail, String userName, String orgName) {
        UserInfo user = userInfoRepository.findByUserEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));

        user.updateProfile(userName, orgName); // 이름·조직명 먼저 반영

        // 이메일(로그인 ID) 변경 처리 — 값이 실제로 바뀐 경우에만 중복 검사 후 반영
        //  - equalsIgnoreCase: 대소문자 무시 비교 (같은 이메일이면 변경으로 안 봄)
        boolean emailChanged = newEmail != null && !newEmail.equalsIgnoreCase(currentEmail);
        if (emailChanged) {
            // 바꾸려는 이메일이 다른 사람이 쓰는 거면 막음
            if (userInfoRepository.existsByUserEmail(newEmail)) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
            }
            user.changeEmail(newEmail); // 이메일 교체(도메인 메서드)
        }

        userInfoRepository.save(user); // 변경 내용 저장(UPDATE)
        return emailChanged;           // true면 컨트롤러가 세션 만료→재로그인 유도
    }


    // ── 비밀번호 변경 (마이페이지) ──
    // 현재 비번이 맞는지 먼저 확인한 뒤 새 비번으로 교체
    @Override
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
        // BCryptPasswordEncoder는 복호화가 불가하다 이유는 BCrypt는 단방향 해시라 원문으로 되돌릴 수 없다.
        // salt가 포함되어 같은 비밀번호라도 매번 다른 해시가 나옴
        // 따라서 검증은 복호화가 아닌 matches() 로 비교하는 방식이다.
        //  - matches(입력한 현재비번, DB에 저장된 해시) → 일치하지 않으면 예외
        if (!passwordEncoder.matches(currentPassword, user.getUserPwd())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
        }
        user.changePassword(passwordEncoder.encode(newPassword)); // 새 비번도 암호화해서 교체
        userInfoRepository.save(user);
    }

    // ── 이메일로 회원 조회 (DTO 반환) ──
    // 화면 표시용. 엔티티 → UserInfoDto(비밀번호 제외)로 변환해 반환
    @Override
    public UserInfoDto getUserDtoByEmail(String userEmail) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail).orElse(null); // 없으면 null
        return UserInfoDto.from(user); // 엔티티 → DTO 변환 (비번 제외)
    }

    // ── 이메일로 회원 엔티티 조회 ──
    // 내부 비즈니스 로직용(원본 엔티티 그대로). 예: LoginFailureHandler가 실패횟수 읽을 때
    @Override
    public UserInfo getEntityByEmail(String userEmail) {
        return userInfoRepository.findByUserEmail(userEmail).orElse(null);
    }

    // ──    로그인 실패 기록    ── (LoginFailureHandler가 호출 / 5회 잠금의 실제 구현)
    @Override
    public void recordLoginFailure(String userEmail) {
        // 존재하지 않는 이메일은 조용히 무시 (User Enumeration 방지)
        //  - "그 이메일 없습니다" 같은 반응을 주면 공격자가 가입된 이메일을 알아낼 수 있어 막음
        UserInfo user = userInfoRepository.findByUserEmail(userEmail).orElse(null);
        if (user == null) return;

        user.incrementFailCount();                    // 실패 횟수 +1 (도메인 메서드)
        if (user.getFailCount() >= MAX_LOGIN_FAIL) {  // 5회 이상이면
            user.lockFor(LOCK_MINUTES);               // 30분 잠금(lockedUntil = now+30분)
        }
        userInfoRepository.save(user);                // 변경된 실패횟수/잠금시각 저장(UPDATE)
    }

    // ── 로그인 실패 초기화 ── (LoginSuccessHandler가 호출 / 로그인 성공 시)
    @Override
    public void resetLoginFailures(String userEmail) {
        UserInfo user = userInfoRepository.findByUserEmail(userEmail).orElse(null);
        if (user == null) return;
        user.resetLoginFailures();     // failCount=0, lockedUntil=null (도메인 메서드)
        userInfoRepository.save(user); // 초기화 결과 저장(UPDATE)
    }

    // ── 이메일 마스킹 ── (아이디 찾기에서 사용)
    // 예: "hong@smartlog.kr" → "ho**@smartlog.kr" (앞 2글자만 노출, 나머지 가림)
    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');          // @ 위치 찾기
        if (atIdx <= 2) return email;            // @ 앞이 2글자 이하면 마스킹 의미 없어 원본 반환
        // 앞 2글자 + "**" + @부터 끝까지
        return email.substring(0, 2) + "**" + email.substring(atIdx);
    }
}
