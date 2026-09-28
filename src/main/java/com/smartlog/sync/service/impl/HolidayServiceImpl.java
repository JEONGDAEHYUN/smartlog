package com.smartlog.sync.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlog.sync.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 공공데이터포털 한국천문연구원 특일 정보 API
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final RestClient restClient;
    // ObjectMapper는 Spring Boot 4.x에서 빈 자동등록이 안 되므로 직접 생성 (스레드 안전)
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${holiday.api-key:}")
    private String apiKey;

    // 연·월 단위 인메모리 캐시 — 최대 24개월분만 유지 (오래된 항목 자동 제거)
    private final Map<String, Map<Integer, String>> cache = new java.util.LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Map<Integer, String>> eldest) {
            return size() > 24;
        }
    };

    private static final String BASE_URL =
            "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";

    @Override
    public Map<Integer, String> getHolidays(int year, int month) {
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of();
        }

        String cacheKey = year + "-" + String.format("%02d", month);
        if (cache.containsKey(cacheKey)) {
            log.debug("[공휴일 API] 캐시 히트 {}", cacheKey);
            return cache.get(cacheKey);
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("solYear", year)
                    .queryParam("solMonth", String.format("%02d", month))
                    .queryParam("numOfRows", 30)
                    .queryParam("_type", "json")
                    .build(true)   // 이미 인코딩된 serviceKey 재인코딩 방지
                    .toUri();

            String body = restClient.get().uri(uri).retrieve().body(String.class);
            Map<Integer, String> holidays = parse(body);
            cache.put(cacheKey, holidays);
            log.info("[공휴일 API] {}-{} API 호출 완료 {}건", year, month, holidays.size());
            return holidays;

        } catch (Exception e) {
            log.warn("[공휴일 API] 호출 실패 {}-{}: {}", year, month, e.getMessage());
            return Map.of();
        }
    }

    // JSON 응답 파싱 — item이 단일 객체·배열·없음 세 케이스 모두 처리
    private Map<Integer, String> parse(String json) throws Exception {
        Map<Integer, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return result;

        JsonNode root = objectMapper.readTree(json);
        JsonNode items = root.path("response").path("body").path("items");

        if (items.isMissingNode() || items.isNull() || items.isTextual()) {
            return result; // 해당 월 공휴일 없음
        }

        JsonNode item = items.path("item");

        if (item.isArray()) {
            // 공휴일이 여러 개
            for (JsonNode node : item) {
                addHoliday(result, node);
            }
        } else if (item.isObject()) {
            // 공휴일이 정확히 1개 — 공공데이터포털 특이 케이스
            addHoliday(result, item);
        }

        return result;
    }

    private void addHoliday(Map<Integer, String> map, JsonNode node) {
        JsonNode locdate = node.path("locdate");
        JsonNode dateName = node.path("dateName");
        JsonNode isHoliday = node.path("isHoliday");

        if (!locdate.isMissingNode() && "Y".equals(isHoliday.asText())) {
            // locdate: yyyyMMdd (예: 20260929)
            int day = locdate.asInt() % 100;
            map.put(day, dateName.asText());
        }
    }
}