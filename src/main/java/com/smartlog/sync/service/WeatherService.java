package com.smartlog.sync.service;

import com.smartlog.sync.dto.CurrentWeatherDto;

// 날씨 API 서비스 인터페이스 (기상청 초단기실황 + 초단기예보)
public interface WeatherService {

    // 서울 강서구 기준 현재 날씨 정보 조회 (DTO 반환)
    CurrentWeatherDto getSeoulWeather(String apiKey);
}
