package com.smartlog.sync.service;

import jakarta.servlet.http.HttpSession;

// 이메일 인증 서비스 인터페이스 — HttpSession TTL 방식 + Gmail SMTP 발송
public interface EmailVerificationService {

    // 인증 코드 생성 + 세션 저장 + 이메일 발송
    String generateCode(HttpSession session, String email);

    // 인증 코드 검증
    boolean verifyCode(HttpSession session, String email, String inputCode);

    // 인증 완료 여부 확인
    boolean isVerified(HttpSession session, String email);

    // 인증 완료 플래그 제거
    void clearVerification(HttpSession session, String email);
}
