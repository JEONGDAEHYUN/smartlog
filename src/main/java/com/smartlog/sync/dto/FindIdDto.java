package com.smartlog.sync.dto;

import jakarta.validation.constraints.NotBlank;

// 아이디(이메일) 찾기 요청 DTO (immutable record)
public record FindIdDto(

        @NotBlank(message = "이름을 입력해주세요")
        String userName,

        @NotBlank(message = "조직명을 입력해주세요")
        String orgName
) {
    // Thymeleaf 폼 초기 바인딩용 빈 인스턴스
    public static FindIdDto empty() {
        return new FindIdDto(null, null);
    }
}