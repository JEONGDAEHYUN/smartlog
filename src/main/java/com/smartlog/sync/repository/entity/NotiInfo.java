package com.smartlog.sync.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 알림정보 Entity
@Entity
@Table(name = "NOTI_INFO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotiInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTI_ID")
    private Long notiId; // 알림 고유번호 (PK, Auto Increment)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCH_ID", nullable = false)
    private SchInfo schInfo; // 연결된 일정 (FK → SCH_INFO)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserInfo userInfo; // 알림 대상 사용자 (FK → USER_INFO)

    @Column(name = "NOTI_MSG", length = 500, nullable = false)
    private String notiMsg; // 알림 메시지

    @Column(name = "IS_READ", length = 1, nullable = false)
    private String isRead; // 읽음 여부 (Y / N)

    @Column(name = "IS_SENT", length = 1, nullable = false)
    private String isSent; // 발송 여부 (Y / N) — 중복 발송 방지용

    @Column(name = "SENT_DT")
    private LocalDateTime sentDt; // 발송 완료 시각

    @Column(name = "NOTI_DT", nullable = false)
    private LocalDateTime notiDt; // 발송 기준 시간

    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt; // 등록일

    // Entity 저장 시 기본값 설정
    @PrePersist
    protected void onCreate() {
        this.regDt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = "N";
        if (this.isSent == null) this.isSent = "N";
    }
}
