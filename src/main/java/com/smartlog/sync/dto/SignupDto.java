package com.smartlog.sync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입 요청 DTO (immutable record, 폼 바인딩 전용 — Builder 불필요)
public record SignupDto(

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String userEmail,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다"
        )
        String userPwd,

        @NotBlank(message = "이름을 입력해주세요")
        String userName,

        @NotBlank(message = "조직명을 입력해주세요")
        String orgName
) {
    // Thymeleaf 폼 초기 바인딩용 빈 인스턴스
    public static SignupDto empty() {
        return new SignupDto(null, null, null, null);
    }
}