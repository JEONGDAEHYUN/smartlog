package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 회원정보 응답 DTO (비밀번호 제외 — 화면 노출 차단)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {

    private Long userId;
    private String userEmail;
    private String userName;
    private String orgName;
    private String userRole;
    private LocalDateTime regDt;

    // Entity → DTO 변환 (정적 팩토리 메서드)
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
