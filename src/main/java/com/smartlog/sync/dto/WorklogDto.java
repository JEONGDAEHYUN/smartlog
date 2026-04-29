package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.Worklog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// 업무일지 응답 DTO (MongoDB Worklog → 화면 전달용)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorklogDto {

    private String logId;
    private Long userId;
    private String rawContent;
    private String refinedContent;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity → DTO 변환
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
