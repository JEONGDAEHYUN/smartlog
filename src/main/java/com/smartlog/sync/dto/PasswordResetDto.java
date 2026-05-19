package com.smartlog.sync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 비밀번호 재설정 요청 DTO (이메일 인증 후)
@Data
public class PasswordResetDto {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String userEmail;

    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
        message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다"
    )
    private String newPassword;

    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String confirmPassword;

    public boolean isPasswordMatched() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
