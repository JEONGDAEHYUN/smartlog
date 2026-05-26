package com.smartlog.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Gemini API 응답 파싱용 DTO (immutable record + 중첩 record)
// 응답 구조: { candidates: [ { content: { parts: [ { text: "..." } ] } } ] }
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponseDto(List<Candidate> candidates) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {}
}