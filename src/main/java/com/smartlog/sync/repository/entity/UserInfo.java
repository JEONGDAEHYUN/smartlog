package com.smartlog.sync.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 회원정보 Entity
@Entity
@Table(name = "USER_INFO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId; // 사용자 고유번호 (PK, Auto Increment)

    @Column(name = "USER_EMAIL", length = 100, nullable = false, unique = true)
    private String userEmail; // 로그인 ID (이메일)

    @Column(name = "USER_PWD", length = 255, nullable = false)
    private String userPwd; // 비밀번호 (BCrypt 암호화 저장)

    @Column(name = "USER_NAME", length = 100, nullable = false)
    private String userName; // 이름

    @Column(name = "ORG_NAME", length = 100, nullable = false)
    private String orgName; // 조직명

    @Column(name = "USER_ROLE", length = 20, nullable = false)
    private String userRole; // 권한 (ROLE_USER / ROLE_ADMIN)

    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt; // 등록일

    @Builder.Default
    @Column(name = "FAIL_COUNT", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer failCount = 0; // 로그인 연속 실패 횟수 (기존 레코드는 0으로 자동 보정)

    @Column(name = "LOCKED_UNTIL")
    private LocalDateTime lockedUntil; // 계정 잠금 해제 시각 (null이면 잠금 아님)

    // Entity 저장 시 등록일 자동 설정
    @PrePersist
    protected void onCreate() {
        this.regDt = LocalDateTime.now();
        if (this.failCount == null) this.failCount = 0;
    }

    // 비밀번호 변경 — BCrypt 인코딩된 값만 받는다
    public void changePassword(String encodedPassword) {
        this.userPwd = encodedPassword;
    }

    // 프로필(이름·조직) 일괄 수정
    public void updateProfile(String userName, String orgName) {
        this.userName = userName;
        this.orgName = orgName;
    }

    // 이메일(로그인 ID) 변경 — 중복 검사는 Service 계층에서 선행
    public void changeEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    // 로그인 실패 카운트 1 증가
    public void incrementFailCount() {
        this.failCount = (this.failCount == null ? 0 : this.failCount) + 1;
    }

    // 계정을 minutes 분 동안 잠금
    public void lockFor(int minutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(minutes);
    }

    // 로그인 성공 또는 잠금 해제 시 — 실패 카운트/잠금 해제
    public void resetLoginFailures() {
        this.failCount = 0;
        this.lockedUntil = null;
    }
}
