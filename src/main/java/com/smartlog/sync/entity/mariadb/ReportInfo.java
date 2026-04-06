package com.smartlog.sync.entity.mariadb;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 보고서정보 Entity
@Entity
@Table(name = "REPORT_INFO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REP_ID")
    private Long repId; // 보고서 고유번호 (PK, Auto Increment)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserInfo userInfo; // 작성자 (FK → USER_INFO)

    @Column(name = "REP_TITLE", nullable = false)
    private String repTitle; // 보고서 제목 (AI 생성)

    @Column(name = "REP_CONT", columnDefinition = "TEXT", nullable = false)
    private String repCont; // 보고서 본문 (TEXT — 긴 내용)

    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt; // 생성 일시

    // Entity 저장 시 등록일 자동 설정
    @PrePersist
    protected void onCreate() {
        this.regDt = LocalDateTime.now();
    }
}
