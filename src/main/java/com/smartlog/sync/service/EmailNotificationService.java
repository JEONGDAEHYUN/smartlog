package com.smartlog.sync.service;

// 알림 이메일 발송 인터페이스
public interface EmailNotificationService {

    // 알림 메시지를 지정 이메일로 발송
    void sendNotiEmail(String toEmail, String notiMsg);
}