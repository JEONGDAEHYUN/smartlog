package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.ReportInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// 보고서 응답 DTO (UserInfo 관계 제외)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportInfoDto {

    private Long repId;
    private String repTitle;
    private String repCont;
    private LocalDateTime regDt;

    // Entity → DTO 변환
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
