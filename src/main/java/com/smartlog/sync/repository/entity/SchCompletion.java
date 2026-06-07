package com.smartlog.sync.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 반복 일정의 사용자별 완료 기록 (가상 이벤트 추적용)
// SchInfo 와는 sch_id 만으로 느슨하게 연결 (양방향 연관관계 무한 루프 회피)
@Entity
@Table(
        name = "SCH_COMPLETION",
        uniqueConstraints = @UniqueConstraint(name = "uk_sch_completion", columnNames = {"SCH_ID", "COMPLETION_DATE"})
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPLETION_ID")
    private Long completionId;

    @Column(name = "SCH_ID", nullable = false)
    private Long schId;                  // SCH_INFO(SCH_ID) 참조 (DDL 에 ON DELETE CASCADE)

    @Column(name = "COMPLETION_DATE", nullable = false)
    private LocalDate completionDate;    // 사용자가 완료 처리한 날짜

    @Column(name = "COMPLETED_AT", nullable = false)
    private LocalDateTime completedAt;   // 클릭 시각 (감사용)

    @PrePersist
    protected void onCreate() {
        if (this.completedAt == null) this.completedAt = LocalDateTime.now();
    }
}