package com.smartlog.sync.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 회원정보 Entity
@Entity
@Table(name = "USER_INFO")
@Getter
@Setter
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

    // Entity 저장 시 등록일 자동 설정
    @PrePersist
    protected void onCreate() {
        this.regDt = LocalDateTime.now();
    }
}
