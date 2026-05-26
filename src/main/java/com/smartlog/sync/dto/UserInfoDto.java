package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.UserInfo;
import lombok.Builder;

import java.time.LocalDateTime;

// 회원정보 응답 DTO (비밀번호 제외 — 화면 노출 차단, immutable record + Builder)
@Builder
public record UserInfoDto(
        Long userId,
        String userEmail,
        String userName,
        String orgName,
        String userRole,
        LocalDateTime regDt
) {
    // Entity → DTO 변환 (정적 팩토리 메서드, Builder 패턴 일관 적용)
    public static UserInfoDto from(UserInfo entity) {
        if (entity == null) return null;
        return UserInfoDto.builder()
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .userName(entity.getUserName())
                .orgName(entity.getOrgName())
                .userRole(entity.getUserRole())
                .regDt(entity.getRegDt())
                .build();
    }
}