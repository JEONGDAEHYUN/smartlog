package com.smartlog.sync.service.impl;

import com.smartlog.sync.dto.CurrentWeatherDto;
import com.smartlog.sync.dto.WeatherApiResponseDto;
import com.smartlog.sync.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 날씨 API 서비스 구현체 (기상청 초단기실황 + 초단기예보)
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final RestClient restClient;

    // 기상청 초단기실황 API (기온, 습도, 풍속, 강수형태)
    private static final String NCST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst"
                    + "?serviceKey=%s&numOfRows=10&pageNo=1&dataType=JSON"
                    + "&base_date=%s&base_time=%s&nx=58&ny=126";

    // 기상청 초단기예보 API (하늘상태 SKY)
    private static final String FCST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst"
                    + "?serviceKey=%s&numOfRows=60&pageNo=1&dataType=JSON"
                    + "&base_date=%s&base_time=%s&nx=58&ny=126";

    // 한국폴리텍대학 서울강서캠퍼스(서울 강서구 등촌동) 기준 격자 좌표: nx=58, ny=126

    @Override
    public CurrentWeatherDto getSeoulWeather(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("YOUR_")) {
            return getDefaultWeather();
        }

        try {
            // 초단기실황 호출 — 기온, 습도, 풍속, 강수형태
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime ncstBase = (now.getMinute() < 40) ? now.minusHours(1) : now;
            String ncstDate = ncstBase.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String ncstHour = ncstBase.format(DateTimeFormatter.ofPattern("HH")) + "00";

            // URI 객체로 생성하여 RestClient의 이중 인코딩 방지
            URI ncstUri = URI.create(String.format(NCST_URL, apiKey, ncstDate, ncstHour));
            WeatherApiResponseDto ncstResponse = restClient.get()
                    .uri(ncstUri)
                    .retrieve()
                    .body(WeatherApiResponseDto.class);

            String temp = "--";
            int humidity = 0;
            String windSpeed = "--";
            int pty = 0;

            List<WeatherApiResponseDto.Item> ncstItems = extractItems(ncstResponse);
            if (ncstItems != null) {
                for (WeatherApiResponseDto.Item item : ncstItems) {
                    String value = item.getObsrValue();
                    switch (item.getCategory()) {
                        case "T1H" -> temp = String.format("%.0f", Double.parseDouble(value));
                        case "REH" -> humidity = (int) Double.parseDouble(value);
                        case "WSD" -> windSpeed = String.format("%.1f", Double.parseDouble(value));
                        case "PTY" -> pty = (int) Double.parseDouble(value);
                    }
                }
            }

            // 초단기예보 호출 — 하늘상태(SKY)
            LocalDateTime fcstBase = (now.getMinute() < 45) ? now.minusHours(1) : now;
            String fcstDate = fcstBase.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fcstHour = fcstBase.format(DateTimeFormatter.ofPattern("HH")) + "30";

            int sky = 1; // 기본값: 맑음
            try {
                URI fcstUri = URI.create(String.format(FCST_URL, apiKey, fcstDate, fcstHour));
                WeatherApiResponseDto fcstResponse = restClient.get()
                        .uri(fcstUri)
                        .retrieve()
                        .body(WeatherApiResponseDto.class);

                List<WeatherApiResponseDto.Item> fcstItems = extractItems(fcstResponse);
                if (fcstItems != null) {
                    for (WeatherApiResponseDto.Item item : fcstItems) {
                        if ("SKY".equals(item.getCategory())) {
                            sky = (int) Double.parseDouble(item.getFcstValue());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("초단기예보 API 호출 실패 (하늘상태 기본값 사용): {}", e.getMessage());
            }

            return CurrentWeatherDto.builder()
                    .city("서울 강서구")
                    .temp(temp)
                    .description(mapDescription(pty, sky))
                    .humidity(humidity)
                    .windSpeed(windSpeed)
                    .icon(mapIcon(pty, sky))
                    .build();

        } catch (Exception e) {
            log.warn("기상청 API 호출 실패: {}", e.getMessage());
            return getDefaultWeather();
        }
    }

    // 응답 DTO에서 item 리스트 안전 추출
    private List<WeatherApiResponseDto.Item> extractItems(WeatherApiResponseDto response) {
        if (response == null || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null) {
            return null;
        }
        return response.getResponse().getBody().getItems().getItem();
    }

    // API 호출 실패 시 사용할 기본값
    private CurrentWeatherDto getDefaultWeather() {
        return CurrentWeatherDto.builder()
                .city("서울 강서구")
                .temp("--")
                .description("날씨 정보를 불러올 수 없습니다")
                .humidity(0)
                .windSpeed("--")
                .icon("🌤️")
                .build();
    }

    // 강수형태(PTY) + 하늘상태(SKY) → 날씨 설명
    private String mapDescription(int pty, int sky) {
        if (pty > 0) {
            return switch (pty) {
                case 1 -> "비";
                case 2 -> "비/눈";
                case 3 -> "눈";
                case 5 -> "빗방울";
                case 6 -> "빗방울눈날림";
                case 7 -> "눈날림";
                default -> "비";
            };
        }
        return switch (sky) {
            case 1 -> "맑음";
            case 3 -> "구름많음";
            case 4 -> "흐림";
            default -> "맑음";
        };
    }

    // 강수형태(PTY) + 하늘상태(SKY) → 아이콘
    private String mapIcon(int pty, int sky) {
        if (pty > 0) {
            return switch (pty) {
                case 1, 5 -> "🌧️";
                case 2, 6 -> "🌧️";
                case 3, 7 -> "🌨️";
                default -> "🌧️";
            };
        }
        return switch (sky) {
            case 1 -> "☀️";
            case 3 -> "⛅";
            case 4 -> "☁️";
            default -> "☀️";
        };
    }
}