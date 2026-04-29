package com.smartlog.sync.service.impl;

import com.smartlog.sync.service.EmailVerificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

// 이메일 인증 서비스 구현체 — HttpSession TTL 방식 + Gmail SMTP 발송
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int TTL_SECONDS = 600; // 10분

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public String generateCode(HttpSession session, String email) {
        String code = generateRandomCode();
        session.setAttribute("VERIFY_CODE_" + email, code);
        session.setAttribute("VERIFY_TIME_" + email, System.currentTimeMillis());
        session.setMaxInactiveInterval(TTL_SECONDS);

        sendVerificationEmail(email, code);

        return code;
    }

    // 인증 코드 메일 발송
    private void sendVerificationEmail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[SmartLog] 이메일 인증 코드");
            message.setText(
                    "안녕하세요, SmartLog 입니다.\n\n" +
                    "이메일 인증 코드는 다음과 같습니다:\n\n" +
                    "    " + code + "\n\n" +
                    "본 코드는 10분간 유효합니다.\n" +
                    "본인이 요청하지 않으셨다면 이 메일을 무시해주세요.\n"
            );
            mailSender.send(message);
            log.info("인증 메일 발송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("인증 메일 발송 실패: {} / {}", toEmail, e.getMessage());
            throw new IllegalStateException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @Override
    public boolean verifyCode(HttpSession session, String email, String inputCode) {
        String savedCode = (String) session.getAttribute("VERIFY_CODE_" + email);
        Long savedTime = (Long) session.getAttribute("VERIFY_TIME_" + email);

        if (savedCode == null || savedTime == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - savedTime;
        if (elapsed > TTL_SECONDS * 1000L) {
            session.removeAttribute("VERIFY_CODE_" + email);
            session.removeAttribute("VERIFY_TIME_" + email);
            return false;
        }

        if (savedCode.equals(inputCode)) {
            session.setAttribute("VERIFIED_" + email, true);
            session.removeAttribute("VERIFY_CODE_" + email);
            session.removeAttribute("VERIFY_TIME_" + email);
            return true;
        }

        return false;
    }

    @Override
    public boolean isVerified(HttpSession session, String email) {
        Boolean verified = (Boolean) session.getAttribute("VERIFIED_" + email);
        return verified != null && verified;
    }

    @Override
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
