package com.smartlog.sync.service;

import com.smartlog.sync.repository.entity.SchInfo;

// 알림 비즈니스 로직 인터페이스
public interface NotificationService {

    // 일정 등록 시 알림 1개 생성 (1시간/30분/15분 단계)
    void createScheduleNotification(SchInfo sch);

    // 일정 수정 시 기존 알림 삭제 후 재생성
    void updateScheduleNotification(SchInfo sch);
}
