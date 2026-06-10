package com.smartlog.sync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 마이페이지 프로필 수정 요청 DTO (immutable record)
public record ProfileUpdateDto(

        // 이메일(로그인 ID) — 변경 가능. 변경 시 중복 검사 + 재로그인 유도
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String userEmail,

        @NotBlank(message = "이름을 입력해주세요")
        String userName,

        @NotBlank(message = "조직명을 입력해주세요")
        String orgName
) {}