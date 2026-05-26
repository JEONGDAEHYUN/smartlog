package com.smartlog.sync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

// 일정 등록/수정 요청 DTO (immutable record)
@Builder
public record ScheduleDto(

        Long schId,                              // 수정 시 사용

        @NotBlank(message = "일정 제목을 입력해주세요")
        String schTitle,

        @NotNull(message = "시작 일시를 선택해주세요")
        LocalDateTime startDt,

        LocalDateTime endDt,                     // NULL 허용 (진행중 일정)

        @NotBlank(message = "우선순위를 선택해주세요")
        String priority,                         // HIGH / MID / LOW

        @NotBlank(message = "상태를 선택해주세요")
        String status,                           // PLANNED / IN_PROGRESS / DONE

        String logId,                            // MongoDB WORKLOG 연결용 (선택)

        String recurring,                        // 반복 주기 (매일/매주 월요일/매월 등, 빈값이면 단건)

        String schMemo                           // 메모 (특이사항, 내일 예정 등)
) {
    // logId 만 교체한 새 인스턴스 반환 (record는 immutable — wither 패턴)
    public ScheduleDto withLogId(String newLogId) {
        return new ScheduleDto(schId, schTitle, startDt, endDt, priority, status, newLogId, recurring, schMemo);
    }

    // Thymeleaf 폼 초기 바인딩용 빈 인스턴스
    public static ScheduleDto empty() {
        return new ScheduleDto(null, null, null, null, null, null, null, null, null);
    }
}