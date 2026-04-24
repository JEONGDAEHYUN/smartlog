package com.smartlog.sync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 날씨 API 서비스 (기상청 초단기실황 + 초단기예보)
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final RestTemplate restTemplate;

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

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSeoulWeather(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("YOUR_")) {
            return getDefaultWeather();
        }

        try {
            // 초단기실황: 매시 40분 이후 정시 데이터 제공
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime ncstBase;
            if (now.getMinute() < 40) {
                ncstBase = now.minusHours(1);
            } else {
                ncstBase = now;
            }

            String ncstDate = ncstBase.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String ncstHour = ncstBase.format(DateTimeFormatter.ofPattern("HH")) + "00";

            // 초단기실황 호출 — 기온, 습도, 풍속, 강수형태
            // URI 객체로 생성하여 RestTemplate의 이중 인코딩 방지
            URI ncstUri = URI.create(String.format(NCST_URL, apiKey, ncstDate, ncstHour));
            Map<String, Object> ncstResponse = restTemplate.getForObject(ncstUri, Map.class);

            String temp = "--";
            int humidity = 0;
            String windSpeed = "--";
            int pty = 0;

            if (ncstResponse != null) {
                List<Map<String, Object>> ncstItems = extractItems(ncstResponse);
                if (ncstItems != null) {
                    for (Map<String, Object> item : ncstItems) {
                        String category = (String) item.get("category");
                        String value = String.valueOf(item.get("obsrValue"));

                        switch (category) {
                            case "T1H" -> temp = String.format("%.0f", Double.parseDouble(value));
                            case "REH" -> humidity = (int) Double.parseDouble(value);
                            case "WSD" -> windSpeed = String.format("%.1f", Double.parseDouble(value));
                            case "PTY" -> pty = (int) Double.parseDouble(value);
                        }
                    }
                }
            }

            // 초단기예보 호출 — 하늘상태(SKY) 조회
            // 초단기예보: 매시 30분 발표, 발표 후 +1시간부터 +6시간 예보
            LocalDateTime fcstBase;
            if (now.getMinute() < 45) {
                fcstBase = now.minusHours(1);
            } else {
                fcstBase = now;
            }

            String fcstDate = fcstBase.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fcstHour = fcstBase.format(DateTimeFormatter.ofPattern("HH")) + "30";

            int sky = 1; // 기본값: 맑음
            try {
                URI fcstUri = URI.create(String.format(FCST_URL, apiKey, fcstDate, fcstHour));
                Map<String, Object> fcstResponse = restTemplate.getForObject(fcstUri, Map.class);

                if (fcstResponse != null) {
                    List<Map<String, Object>> fcstItems = extractItems(fcstResponse);
                    if (fcstItems != null) {
                        // 가장 가까운 시간의 SKY 값 추출
                        for (Map<String, Object> item : fcstItems) {
                            if ("SKY".equals(item.get("category"))) {
                                sky = (int) Double.parseDouble(String.valueOf(item.get("fcstValue")));
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("초단기예보 API 호출 실패 (하늘상태 기본값 사용): {}", e.getMessage());
            }

            String description = mapDescription(pty, sky);
            String icon = mapIcon(pty, sky);

            Map<String, Object> result = new HashMap<>();
            result.put("city", "서울 강서구");
            result.put("temp", temp);
            result.put("description", description);
            result.put("humidity", humidity);
            result.put("windSpeed", windSpeed);
            result.put("icon", icon);
            return result;

        } catch (Exception e) {
            log.warn("기상청 API 호출 실패: {}", e.getMessage());
            return getDefaultWeather();
        }
    }

    // 응답에서 item 배열 추출
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> response) {
        try {
            Map<String, Object> body = (Map<String, Object>)
                    ((Map<String, Object>) response.get("response")).get("body");
            Map<String, Object> items = (Map<String, Object>) body.get("items");
            return (List<Map<String, Object>>) items.get("item");
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> getDefaultWeather() {
        Map<String, Object> result = new HashMap<>();
        result.put("city", "서울 강서구");
        result.put("temp", "--");
        result.put("description", "날씨 정보를 불러올 수 없습니다");
        result.put("humidity", 0);
        result.put("windSpeed", "--");
        result.put("icon", "🌤️");
        return result;
    }

    // 강수형태(PTY) + 하늘상태(SKY) → 날씨 설명
    private String mapDescription(int pty, int sky) {
        // 강수가 있으면 강수형태 우선
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
        // 강수 없으면 하늘상태 표시
        return switch (sky) {
            case 1 -> "맑음";
            case 3 -> "구름많음";
            case 4 -> "흐림";
            default -> "맑음";
        };
    }

    // 강수형태(PTY) + 하늘상태(SKY) → 아이콘
    private String mapIcon(int pty, int sky) {
        // 강수가 있으면 강수 아이콘
        if (pty > 0) {
            return switch (pty) {
                case 1, 5 -> "🌧️";
                case 2, 6 -> "🌧️";
                case 3, 7 -> "🌨️";
                default -> "🌧️";
            };
        }
        // 강수 없으면 하늘상태 아이콘
        return switch (sky) {
            case 1 -> "☀️";       // 맑음
            case 3 -> "⛅";       // 구름많음
            case 4 -> "☁️";       // 흐림
            default -> "☀️";
        };
    }
}
