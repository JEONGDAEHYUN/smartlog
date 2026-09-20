package com.smartlog.sync.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입 요청 DTO (immutable record, 폼 바인딩 전용 — Builder 불필요)
// [사전질문 공통 11] 요청 DTO와 응답 DTO 분리 → 이건 "요청 DTO"(입력+검증). 응답 DTO는 UserInfoDto(비번 제외)
// [사전질문 JPA 12] @NotBlank/@NotNull/@Email/@Size 차이 → 아래 필드들이 실제 사용 예
public record SignupDto(

        @NotBlank(message = "이메일을 입력해주세요")  // [JPA 12] @NotBlank: null·빈문자·공백 불허
        @Email(message = "올바른 이메일 형식이 아닙니다")  // [JPA 12] @Email: 이메일 형식 검증
        String userEmail,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다")  // [JPA 12] @Size: 길이 범위
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다"
        )
        String userPwd,

        // 비밀번호 확인 — 위 userPwd 와 일치해야 함 (isPasswordMatched() 에서 비교)
        @NotBlank(message = "비밀번호 확인을 입력해주세요")
        String userPwdConfirm,

        @NotBlank(message = "이름을 입력해주세요")
        String userName,

        @NotBlank(message = "조직명을 입력해주세요")
        String orgName,

        // 권한 (ROLE_USER / ROLE_ADMIN) — 폼에서 라디오로 선택, 기본 ROLE_USER
        String userRole
) {
    // 비밀번호 ↔ 비밀번호 확인 일치 검증
    //  - @AssertTrue: 이 메서드가 false 면 @Valid 검사 실패 → 컨트롤러의 bindingResult 가 자동으로 잡음
    //  - 둘 다 입력됐을 때만 비교 (빈값 검사는 위 @NotBlank 가 담당)
    @AssertTrue(message = "비밀번호가 일치하지 않습니다")
    public boolean isPasswordMatched() {
        return userPwd != null && userPwd.equals(userPwdConfirm);
    }

    // 화이트리스트 검증 — yml/DB 오염 방지, 미지정 또는 비허용 값은 ROLE_USER로 강제
    public String userRoleSafe() {
        return "ROLE_ADMIN".equals(userRole) ? "ROLE_ADMIN" : "ROLE_USER";
    }

    // Thymeleaf 폼 초기 바인딩용 빈 인스턴스 (기본 권한 ROLE_USER 선택 상태)
    public static SignupDto empty() {
        return new SignupDto(null, null, null, null, null, "ROLE_USER");
    }
}