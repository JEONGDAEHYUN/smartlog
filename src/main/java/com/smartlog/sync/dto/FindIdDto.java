package com.smartlog.sync.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 아이디(이메일) 찾기 요청 DTO
@Data
public class FindIdDto {

    @NotBlank(message = "이름을 입력해주세요")
    private String userName;

    @NotBlank(message = "조직명을 입력해주세요")
    private String orgName;
}
