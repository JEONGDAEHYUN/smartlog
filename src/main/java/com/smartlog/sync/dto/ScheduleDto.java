package com.smartlog.sync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 일정 등록/수정 요청 DTO
@Getter
@Setter
public class ScheduleDto {

    private Long schId; // 수정 시 사용

    @NotBlank(message = "일정 제목을 입력해주세요")
    private String schTitle;

    @NotNull(message = "시작 일시를 선택해주세요")
    private LocalDateTime startDt;

    private LocalDateTime endDt; // NULL 허용 (진행중 일정)

    @NotBlank(message = "우선순위를 선택해주세요")
    private String priority; // HIGH / MID / LOW

    @NotBlank(message = "상태를 선택해주세요")
    private String status; // PLANNED / IN_PROGRESS / DONE

    private String logId; // MongoDB WORKLOG 연결용 (선택)

    private String recurring; // 반복 주기 (매일/매주 월요일/매월 등, 빈값이면 단건)

    private String schMemo; // 메모 (특이사항, 내일 예정 등)
}
