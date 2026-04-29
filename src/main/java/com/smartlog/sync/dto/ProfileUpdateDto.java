package com.smartlog.sync.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 마이페이지 프로필 수정 요청 DTO
@Data
public class ProfileUpdateDto {

    @NotBlank(message = "이름을 입력해주세요")
    private String userName;

    @NotBlank(message = "조직명을 입력해주세요")
    private String orgName;
}
