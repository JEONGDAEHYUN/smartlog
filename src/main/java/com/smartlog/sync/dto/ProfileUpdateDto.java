package com.smartlog.sync.dto;

import jakarta.validation.constraints.NotBlank;

// 마이페이지 프로필 수정 요청 DTO (immutable record)
public record ProfileUpdateDto(

        @NotBlank(message = "이름을 입력해주세요")
        String userName,

        @NotBlank(message = "조직명을 입력해주세요")
        String orgName
) {}