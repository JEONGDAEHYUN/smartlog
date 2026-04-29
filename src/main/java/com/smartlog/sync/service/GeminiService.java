package com.smartlog.sync.service;

// Gemini API 연동 서비스 인터페이스
public interface GeminiService {

    // 업무 메모/프롬프트 → Gemini 정제 결과 텍스트
    String refineWorklog(String rawContent);
}
