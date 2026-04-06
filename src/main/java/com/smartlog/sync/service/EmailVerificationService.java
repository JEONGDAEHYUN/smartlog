package com.smartlog.sync.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Random;

// 이메일 인증 서비스 — HttpSession TTL 방식 (Redis X)
@Service
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int TTL_SECONDS = 600; // 10분

    // 인증 코드 생성 및 세션 저장
    public String generateCode(HttpSession session, String email) {
        String code = generateRandomCode();
        session.setAttribute("VERIFY_CODE_" + email, code);
        session.setAttribute("VERIFY_TIME_" + email, System.currentTimeMillis());
        session.setMaxInactiveInterval(TTL_SECONDS);
        return code;
    }

    // 인증 코드 검증
    public boolean verifyCode(HttpSession session, String email, String inputCode) {
        String savedCode = (String) session.getAttribute("VERIFY_CODE_" + email);
        Long savedTime = (Long) session.getAttribute("VERIFY_TIME_" + email);

        if (savedCode == null || savedTime == null) {
            return false;
        }

        // 10분 만료 확인
        long elapsed = System.currentTimeMillis() - savedTime;
        if (elapsed > TTL_SECONDS * 1000L) {
            session.removeAttribute("VERIFY_CODE_" + email);
            session.removeAttribute("VERIFY_TIME_" + email);
            return false;
        }

        if (savedCode.equals(inputCode)) {
            // 인증 성공 — 인증 완료 플래그 설정
            session.setAttribute("VERIFIED_" + email, true);
            session.removeAttribute("VERIFY_CODE_" + email);
            session.removeAttribute("VERIFY_TIME_" + email);
            return true;
        }

        return false;
    }

    // 인증 완료 여부 확인
    public boolean isVerified(HttpSession session, String email) {
        Boolean verified = (Boolean) session.getAttribute("VERIFIED_" + email);
        return verified != null && verified;
    }

    // 인증 완료 플래그 제거
    public void clearVerification(HttpSession session, String email) {
        session.removeAttribute("VERIFIED_" + email);
    }

    // 6자리 랜덤 숫자 코드 생성
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
