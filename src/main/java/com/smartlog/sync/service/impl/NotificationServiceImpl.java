package com.smartlog.sync.service.impl;

import com.smartlog.sync.repository.entity.NotiInfo;
import com.smartlog.sync.repository.entity.SchInfo;
import com.smartlog.sync.repository.NotiInfoRepository;
import com.smartlog.sync.repository.SchInfoRepository;
import com.smartlog.sync.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// 알림 서비스 구현체 — 일정 마감 알림 자동 생성/발송 + 스케줄러
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotiInfoRepository notiInfoRepository;
    private final SchInfoRepository schInfoRepository;

    // 매일 자정(00:01)에 반복 업무의 DONE → PLANNED 자동 해제
    @Scheduled(cron = "0 1 0 * * *")
    public void resetRecurringTasks() {
        List<SchInfo> allRecurring = schInfoRepository.findAll().stream()
                .filter(s -> s.getRecurring() != null && "DONE".equals(s.getStatus()))
                .toList();

        for (SchInfo sch : allRecurring) {
            sch.setStatus("PLANNED");
            schInfoRepository.save(sch);
            createScheduleNotification(sch);
            log.info("[반복업무 초기화] schId={}, title={}", sch.getSchId(), sch.getSchTitle());
        }
    }

    @Override
    public void createScheduleNotification(SchInfo sch) {
        if ("DONE".equals(sch.getStatus())) return;

        LocalDateTime baseDt = sch.getEndDt() != null ? sch.getEndDt() : sch.getStartDt();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime notiDt = null;
        String msg = null;

        LocalDateTime noti60 = baseDt.minusMinutes(60);
        LocalDateTime noti30 = baseDt.minusMinutes(30);
        LocalDateTime noti15 = baseDt.minusMinutes(15);

        if (noti60.isAfter(now)) {
            notiDt = noti60;
            msg = "[" + sch.getSchTitle() + "] 마감 1시간 전입니다.";
        } else if (noti30.isAfter(now)) {
            notiDt = noti30;
            msg = "[" + sch.getSchTitle() + "] 마감 30분 전입니다.";
        } else if (noti15.isAfter(now)) {
            notiDt = noti15;
            msg = "[" + sch.getSchTitle() + "] 마감 15분 전입니다.";
        }

        if (notiDt == null) return;

        notiInfoRepository.save(NotiInfo.builder()
                .schInfo(sch).userInfo(sch.getUserInfo())
                .notiMsg(msg).notiDt(notiDt).build());
    }

    @Override
    public void updateScheduleNotification(SchInfo sch) {
        List<NotiInfo> existing = notiInfoRepository.findBySchInfoSchIdAndIsSent(sch.getSchId(), "N");
        notiInfoRepository.deleteAll(existing);
        createScheduleNotification(sch);
    }

    // 매 1분마다 발송 대기 알림 체크 → 발송 + 다음 단계 알림 생성
    @Scheduled(fixedRate = 60000)
    public void checkAndSendNotifications() {
        List<NotiInfo> pendingList = notiInfoRepository.findByIsSent("N");
        LocalDateTime now = LocalDateTime.now();

        for (NotiInfo noti : pendingList) {
            if (noti.getNotiDt().isBefore(now) || noti.getNotiDt().isEqual(now)) {
                noti.setIsSent("Y");
                noti.setSentDt(now);
                notiInfoRepository.save(noti);
                log.info("[알림 발송] notiId={}, msg={}", noti.getNotiId(), noti.getNotiMsg());

                createNextNotification(noti);
            }
        }
    }

    // 발송된 알림의 다음 단계 알림 생성
    private void createNextNotification(NotiInfo sent) {
        SchInfo sch = sent.getSchInfo();
        LocalDateTime baseDt = sch.getEndDt() != null ? sch.getEndDt() : sch.getStartDt();
        LocalDateTime now = LocalDateTime.now();
        String title = sch.getSchTitle();
        String currentMsg = sent.getNotiMsg();

        LocalDateTime nextDt = null;
        String nextMsg = null;

        if (currentMsg.contains("1시간")) {
            LocalDateTime noti30 = baseDt.minusMinutes(30);
            if (noti30.isAfter(now)) {
                nextDt = noti30;
                nextMsg = "[" + title + "] 마감 30분 전입니다.";
            }
        }
        if (currentMsg.contains("30분") || (currentMsg.contains("1시간") && nextDt == null)) {
            LocalDateTime noti15 = baseDt.minusMinutes(15);
            if (noti15.isAfter(now)) {
                nextDt = noti15;
                nextMsg = "[" + title + "] 마감 15분 전입니다.";
            }
        }

        if (nextDt != null) {
            notiInfoRepository.save(NotiInfo.builder()
                    .schInfo(sch).userInfo(sent.getUserInfo())
                    .notiMsg(nextMsg).notiDt(nextDt).build());
        }
    }
}
