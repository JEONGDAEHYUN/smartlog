package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.NotiInfo;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

// 알림 응답 DTO (관계 객체 제외, schId만 포함, immutable record + Builder)
@Builder
public record NotiInfoDto(
        Long notiId,
        Long schId,              // 연결된 일정 ID (관계 객체 대신 ID만)
        String notiMsg,
        String isRead,
        String isSent,
        LocalDateTime sentDt,
        LocalDateTime notiDt,
        LocalDateTime regDt
) {
    // Entity → DTO 변환 (Builder 패턴 일관 적용)
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