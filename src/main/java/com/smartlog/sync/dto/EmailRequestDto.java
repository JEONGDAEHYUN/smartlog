package com.smartlog.sync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 이메일 단일 입력 요청 DTO (인증코드 발송 등)
@Data
public class EmailRequestDto {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String userEmail;
}
