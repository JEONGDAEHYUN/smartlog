package com.smartlog.sync.service.impl;

import com.smartlog.sync.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// 알림 이메일 발송 구현체 — JavaMailSender(Gmail SMTP)로 단순 텍스트 메일 발송
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendNotiEmail(String toEmail, String notiMsg) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[SmartLog] 일정 알림");
            message.setText(notiMsg + "\n\n" +
                    "──────────────────────\n" +
                    "SmartLog 업무일지 시스템\n" +
                    "이 메일은 발신 전용입니다.");
            mailSender.send(message);
            log.info("[이메일 알림 발송] to={}, msg={}", toEmail, notiMsg);
        } catch (Exception e) {
            // 메일 발송 실패가 알림 흐름 전체를 중단하지 않도록 예외 처리
            log.warn("[이메일 알림 발송 실패] to={}, reason={}", toEmail, e.getMessage());
        }
    }
}