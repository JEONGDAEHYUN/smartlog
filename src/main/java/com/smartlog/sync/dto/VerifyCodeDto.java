package com.smartlog.sync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 이메일 인증코드 검증 요청 DTO (회원가입 / 비번찾기 공용, immutable record)
public record VerifyCodeDto(

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String userEmail,

        @NotBlank(message = "인증 코드를 입력해주세요")
        String inputCode
) {}