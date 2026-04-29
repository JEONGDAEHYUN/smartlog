package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.NotiInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// 알림 응답 DTO (관계 객체 제외, schId만 포함)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotiInfoDto {

    private Long notiId;
    private Long schId;            // 연결된 일정 ID (관계 객체 대신 ID만)
    private String notiMsg;
    private String isRead;
    private String isSent;
    private LocalDateTime sentDt;
    private LocalDateTime notiDt;
    private LocalDateTime regDt;

    // Entity → DTO 변환
    public static NotiInfoDto from(NotiInfo entity) {
        if (entity == null) return null;
        return NotiInfoDto.builder()
                .notiId(entity.getNotiId())
                .schId(entity.getSchInfo() != null ? entity.getSchInfo().getSchId() : null)
                .notiMsg(entity.getNotiMsg())
                .isRead(entity.getIsRead())
                .isSent(entity.getIsSent())
                .sentDt(entity.getSentDt())
                .notiDt(entity.getNotiDt())
                .regDt(entity.getRegDt())
                .build();
    }

    public static List<NotiInfoDto> fromList(List<NotiInfo> entities) {
        return entities.stream().map(NotiInfoDto::from).toList();
    }
}
