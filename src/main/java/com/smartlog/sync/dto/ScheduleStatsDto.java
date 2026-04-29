package com.smartlog.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 일정 통계 DTO — 화면 우선순위/상태별 카드 + 차트용
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleStatsDto {

    private long totalCount;       // 전체 일정 수
    private long highCount;        // 우선순위 HIGH
    private long midCount;         // 우선순위 MID
    private long lowCount;         // 우선순위 LOW
    private long doneCount;        // 상태 DONE
    private long inProgressCount;  // 상태 IN_PROGRESS
    private long plannedCount;     // 상태 PLANNED
    private String doneRate;       // 완료율(%) — "%.1f" 포맷 적용

    // SchInfoDto 목록을 받아서 통계 계산
    public static ScheduleStatsDto from(List<SchInfoDto> schedules) {
        long total = schedules.size();
        long high = schedules.stream().filter(s -> "HIGH".equals(s.getPriority())).count();
        long mid = schedules.stream().filter(s -> "MID".equals(s.getPriority())).count();
        long low = schedules.stream().filter(s -> "LOW".equals(s.getPriority())).count();
        long done = schedules.stream().filter(s -> "DONE".equals(s.getStatus())).count();
        long inProgress = schedules.stream().filter(s -> "IN_PROGRESS".equals(s.getStatus())).count();
        long planned = schedules.stream().filter(s -> "PLANNED".equals(s.getStatus())).count();
        double doneRateValue = total > 0 ? (double) done / total * 100 : 0;

        return ScheduleStatsDto.builder()
                .totalCount(total)
                .highCount(high)
                .midCount(mid)
                .lowCount(low)
                .doneCount(done)
                .inProgressCount(inProgress)
                .plannedCount(planned)
                .doneRate(String.format("%.1f", doneRateValue))
                .build();
    }
}
