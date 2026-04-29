package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.SchInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// 일정 응답 DTO (UserInfo 관계 제외 — Lazy Loading + 순환참조 방지)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchInfoDto {

    private Long schId;
    private String logId;
    private String schTitle;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private String priority;
    private String status;
    private String recurring;
    private String schMemo;
    private LocalDateTime regDt;

    // Entity → DTO 변환
    public static SchInfoDto from(SchInfo entity) {
        if (entity == null) return null;
        return SchInfoDto.builder()
                .schId(entity.getSchId())
                .logId(entity.getLogId())
                .schTitle(entity.getSchTitle())
                .startDt(entity.getStartDt())
                .endDt(entity.getEndDt())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .recurring(entity.getRecurring())

                .schMemo(entity.getSchMemo())
                .regDt(entity.getRegDt())
                .build();
    }

    public static List<SchInfoDto> fromList(List<SchInfo> entities) {
        return entities.stream().map(SchInfoDto::from).toList();
    }
}
