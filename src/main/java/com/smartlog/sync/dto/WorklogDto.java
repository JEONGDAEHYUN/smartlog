package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.Worklog;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

// 업무일지 응답 DTO (MongoDB Worklog → 화면 전달용, immutable record + Builder)
@Builder
public record WorklogDto(
        String logId,
        Long userId,
        String rawContent,
        String refinedContent,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Entity → DTO 변환 (Builder 패턴 일관 적용)
    public static WorklogDto from(Worklog entity) {
        if (entity == null) return null;
        return WorklogDto.builder()
                .logId(entity.getLogId())
                .userId(entity.getUserId())
                .rawContent(entity.getRawContent())
                .refinedContent(entity.getRefinedContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static List<WorklogDto> fromList(List<Worklog> entities) {
        return entities.stream().map(WorklogDto::from).toList();
    }
}