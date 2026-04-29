package com.smartlog.sync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 마이페이지 비밀번호 변경 요청 DTO
@Data
public class PasswordChangeDto {

    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String confirmPassword;

    public boolean isPasswordMatched() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
