package com.smartlog.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

// Gemini API 응답 파싱용 DTO
// 응답 구조: { candidates: [ { content: { parts: [ { text: "..." } ] } } ] }
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponseDto {

    private List<Candidate> candidates;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;
    }
}