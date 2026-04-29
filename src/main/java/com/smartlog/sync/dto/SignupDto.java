package com.smartlog.sync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원가입 요청 DTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupDto {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String userEmail;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String userPwd;

    @NotBlank(message = "이름을 입력해주세요")
    private String userName;

    @NotBlank(message = "조직명을 입력해주세요")
    private String orgName;
}
