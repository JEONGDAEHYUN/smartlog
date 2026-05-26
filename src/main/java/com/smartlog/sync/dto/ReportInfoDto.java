package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.ReportInfo;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

// 보고서 응답 DTO (UserInfo 관계 제외, immutable record + Builder)
@Builder
public record ReportInfoDto(
        Long repId,
        String repTitle,
        String repCont,
        LocalDateTime regDt
) {
    // Entity → DTO 변환 (Builder 패턴 일관 적용)
    public static ReportInfoDto from(ReportInfo entity) {
        if (entity == null) return null;
        return ReportInfoDto.builder()
                .repId(entity.getRepId())
                .repTitle(entity.getRepTitle())
                .repCont(entity.getRepCont())
                .regDt(entity.getRegDt())
                .build();
    }

    public static List<ReportInfoDto> fromList(List<ReportInfo> entities) {
        return entities.stream().map(ReportInfoDto::from).toList();
    }
}