package com.smartlog.sync.service;

import com.smartlog.sync.dto.SignupDto;
import com.smartlog.sync.dto.UserInfoDto;
import com.smartlog.sync.repository.entity.UserInfo;

import java.util.List;

// 회원 관련 비즈니스 로직 인터페이스
public interface UserService {

    // 회원가입 처리
    void signup(SignupDto dto);

    // 이메일 중복 확인
    boolean isEmailDuplicate(String email);

    // 아이디(이메일) 찾기 — 이름 + 조직명으로 조회, 마스킹 처리
    String findEmail(String userName, String orgName);

    // 비밀번호 재설정'
    void resetPassword(String userEmail, String newPassword);

    // 개인정보 수정 (이메일, 이름, 조직명) — 이메일 변경 시 true 반환(재로그인 유도용)
    boolean updateProfile(String currentEmail, String newEmail, String userName, String orgName);

    // 비밀번호 변경 (현재 비밀번호 검증 포함)
    void changePassword(String userEmail, String currentPassword, String newPassword);

    // 이메일로 사용자 정보 조회 (DTO 반환 — 비밀번호 제외)
    UserInfoDto getUserDtoByEmail(String userEmail);

    // 이메일로 사용자 엔티티 조회 (내부 비즈니스 로직용)
    UserInfo getEntityByEmail(String userEmail);

    // 로그인 실패 횟수 증가 — 임계값 도달 시 계정 잠금
    void recordLoginFailure(String userEmail);

    // 로그인 실패 횟수 초기화 — 로그인 성공 시 호출
    void resetLoginFailures(String userEmail);

    // [관리자] 전체 회원 목록 조회
    List<UserInfoDto> findAllUsers();

    // [관리자] 계정 잠금/해제 토글 — 현재 잠금이면 해제, 아니면 365일 잠금
    void adminToggleLock(Long targetUserId);

    // [관리자] 권한 전환 — ROLE_USER ↔ ROLE_ADMIN
    void adminToggleRole(Long targetUserId);
}
