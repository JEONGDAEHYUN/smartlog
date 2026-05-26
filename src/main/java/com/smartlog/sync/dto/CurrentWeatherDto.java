package com.smartlog.sync.dto;

import lombok.Builder;

// 화면(대시보드) 표시용 날씨 정보 DTO (immutable record)
@Builder
public record CurrentWeatherDto(
        String city,         // 도시명 (예: "서울 강서구")
        String temp,         // 기온 (예: "18")
        String description,  // 날씨 설명 (예: "맑음", "비")
        int humidity,        // 습도 (%)
        String windSpeed,    // 풍속 (m/s)
        String icon          // 이모지 아이콘
) {}