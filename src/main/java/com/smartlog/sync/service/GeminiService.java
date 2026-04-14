package com.smartlog.sync.service;

import com.smartlog.sync.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

// Gemini API 연동 서비스
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s";

    // 업무 메모를 정제된 업무일지로 변환
    public String refineWorklog(String rawContent) {
        String url = String.format(GEMINI_URL, geminiConfig.getModel(), geminiConfig.getApiKey());

        String prompt = """
                당신은 업무일지 정제 AI입니다.
                아래 원본 메모를 읽고, 정형화된 업무일지로 변환해주세요.

                [변환 규칙]
                1. 아래 형식으로 정리할 것
                2. 불필요한 구어체 제거, 공식적 문체로 변환
                3. [시간] 형태는 그대로 유지
                4. 특이사항과 비고 항목이 있으면 그대로 정리

                [출력 형식]
                ■ 수행 업무
                1. [시간] 업무 내용 — 결과
                2. [시간] 업무 내용 — 결과

                ■ 특이사항
                - 내용

                ■ 비고
                - 내용

                [원본 메모]
                """ + rawContent + """

                [정제된 업무일지]
                """;

        // Gemini API 요청 본문 구성
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return extractText(response.getBody());
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 정제 요청에 실패했습니다: " + e.getMessage());
        }
    }

    // Gemini 응답에서 텍스트 추출
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> responseBody) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Gemini 응답이 비어있습니다");
        }
        Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
        return (String) parts.get(0).get("text");
    }
}
