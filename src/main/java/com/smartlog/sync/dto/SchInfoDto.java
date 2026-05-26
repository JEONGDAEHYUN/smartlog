package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.SchInfo;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

// 일정 응답 DTO (UserInfo 관계 제외 — Lazy Loading + 순환참조 방지, immutable record + Builder)
@Builder
public record SchInfoDto(
        Long schId,
        String logId,
        String schTitle,
        LocalDateTime startDt,
        LocalDateTime endDt,
        String priority,
        String status,
        String recurring,
        String schMemo,
        LocalDateTime regDt
) {
    // Entity → DTO 변환 (Builder 패턴 일관 적용)
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