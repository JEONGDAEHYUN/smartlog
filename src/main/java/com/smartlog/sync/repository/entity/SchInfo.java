package com.smartlog.sync.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 일정정보 Entity
@Entity
@Table(name = "SCH_INFO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCH_ID")
    private Long schId; // 일정 고유번호 (PK, Auto Increment)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserInfo userInfo; // 작성자 (FK → USER_INFO)

    @Column(name = "LOG_ID", length = 100)
    private String logId; // MongoDB WORKLOG 연결용 UUID (Cross-DB)

    @Column(name = "SCH_TITLE", length = 200, nullable = false)
    private String schTitle; // 일정 제목 (Gemini 정제 결과 반영)

    @Column(name = "START_DT", nullable = false)
    private LocalDateTime startDt; // 시작 일시

    @Column(name = "END_DT")
    private LocalDateTime endDt; // 종료 일시 (NULL 허용 — 진행중 일정)

    @Column(name = "PRIORITY", length = 10, nullable = false)
    private String priority; // 우선순위 (HIGH / MID / LOW)

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status; // 일정 상태 (PLANNED / IN_PROGRESS / DONE)

    @Column(name = "RECURRING", length = 30)
    private String recurring; // 반복 주기 (매일/매주 월요일/매월 등, NULL이면 단건)

    @Column(name = "SCH_MEMO", length = 500)
    private String schMemo; // 메모 (특이사항, 내일 예정 등)

    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt; // 등록일

    // Entity 저장 시 등록일 자동 설정
    @PrePersist
    protected void onCreate() {
        this.regDt = LocalDateTime.now();
    }
}
