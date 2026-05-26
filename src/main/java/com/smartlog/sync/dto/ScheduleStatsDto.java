package com.smartlog.sync.dto;

import lombok.Builder;

import java.util.List;

// 일정 통계 DTO — 화면 우선순위/상태별 카드 + 차트용 (immutable record + Builder)
@Builder
public record ScheduleStatsDto(
        long totalCount,       // 전체 일정 수
        long highCount,        // 우선순위 HIGH
        long midCount,         // 우선순위 MID
        long lowCount,         // 우선순위 LOW
        long doneCount,        // 상태 DONE
        long inProgressCount,  // 상태 IN_PROGRESS
        long plannedCount,     // 상태 PLANNED
        String doneRate        // 완료율(%) — "%.1f" 포맷 적용
) {
    // SchInfoDto 목록을 받아서 통계 계산 (Builder 패턴 일관 적용)
    public static ScheduleStatsDto from(List<SchInfoDto> schedules) {
        long total = schedules.size();
        long high = schedules.stream().filter(s -> "HIGH".equals(s.priority())).count();
        long mid = schedules.stream().filter(s -> "MID".equals(s.priority())).count();
        long low = schedules.stream().filter(s -> "LOW".equals(s.priority())).count();
        long done = schedules.stream().filter(s -> "DONE".equals(s.status())).count();
        long inProgress = schedules.stream().filter(s -> "IN_PROGRESS".equals(s.status())).count();
        long planned = schedules.stream().filter(s -> "PLANNED".equals(s.status())).count();
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