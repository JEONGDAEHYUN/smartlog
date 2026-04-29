package com.smartlog.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 화면(대시보드) 표시용 날씨 정보 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentWeatherDto {

    private String city;          // 도시명 (예: "서울 강서구")
    private String temp;          // 기온 (예: "18")
    private String description;   // 날씨 설명 (예: "맑음", "비")
    private int humidity;         // 습도 (%)
    private String windSpeed;     // 풍속 (m/s)
    private String icon;          // 이모지 아이콘
}
