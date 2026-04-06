package com.smartlog.sync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// 날씨 API 서비스 (OpenWeatherMap)
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final RestTemplate restTemplate;

    // OpenWeatherMap 무료 API (API 키 불필요한 버전은 없으므로 기본값 반환)
    // 실제 연동 시 application.yml에 api-key 추가 필요
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather?q=Seoul&appid=%s&units=metric&lang=kr";

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSeoulWeather(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("YOUR_")) {
            return getDefaultWeather();
        }

        try {
            String url = String.format(API_URL, apiKey);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return getDefaultWeather();

            Map<String, Object> main = (Map<String, Object>) response.get("main");
            var weatherList = (java.util.List<Map<String, Object>>) response.get("weather");
            Map<String, Object> wind = (Map<String, Object>) response.get("wind");

            double temp = ((Number) main.get("temp")).doubleValue();
            int humidity = ((Number) main.get("humidity")).intValue();
            String description = (String) weatherList.get(0).get("description");
            String icon = mapIcon((String) weatherList.get(0).get("main"));
            double windSpeed = ((Number) wind.get("speed")).doubleValue();

            return Map.of(
                    "city", "서울특별시",
                    "temp", String.format("%.0f", temp),
                    "description", description,
                    "humidity", humidity,
                    "windSpeed", String.format("%.1f", windSpeed),
                    "icon", icon
            );
        } catch (Exception e) {
            log.warn("날씨 API 호출 실패: {}", e.getMessage());
            return getDefaultWeather();
        }
    }

    private Map<String, Object> getDefaultWeather() {
        return Map.of(
                "city", "서울특별시",
                "temp", "--",
                "description", "날씨 정보를 불러올 수 없습니다",
                "humidity", 0,
                "windSpeed", "--",
                "icon", "🌤️"
        );
    }

    private String mapIcon(String main) {
        return switch (main) {
            case "Clear" -> "☀️";
            case "Clouds" -> "☁️";
            case "Rain", "Drizzle" -> "🌧️";
            case "Thunderstorm" -> "⛈️";
            case "Snow" -> "🌨️";
            case "Mist", "Fog", "Haze" -> "🌫️";
            default -> "🌤️";
        };
    }
}
