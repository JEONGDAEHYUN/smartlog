package com.smartlog.sync.service;

import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.entity.mariadb.SchInfo;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.repository.mariadb.SchInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

// 일정 관련 비즈니스 로직
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final SchInfoRepository schInfoRepository;
    private final NotificationService notificationService;

    // 일정 등록
    public SchInfo create(UserInfo user, ScheduleDto dto) {
        String recurring = (dto.getRecurring() != null && !dto.getRecurring().isBlank()) ? dto.getRecurring() : null;
        SchInfo sch = SchInfo.builder()
                .userInfo(user)
                .schTitle(dto.getSchTitle())
                .startDt(dto.getStartDt())
                .endDt(dto.getEndDt())
                .priority(dto.getPriority())
                .status(dto.getStatus())
                .logId(dto.getLogId())
                .recurring(recurring)
                .schMemo(dto.getSchMemo())
                .build();
        SchInfo saved = schInfoRepository.save(sch);
        notificationService.createScheduleNotification(saved);
        return saved;
    }

    // 일정 수정
    public SchInfo update(Long schId, ScheduleDto dto) {
        SchInfo sch = schInfoRepository.findById(schId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다"));
        sch.setSchTitle(dto.getSchTitle());
        sch.setStartDt(dto.getStartDt());
        sch.setEndDt(dto.getEndDt());
        sch.setPriority(dto.getPriority());
        sch.setStatus(dto.getStatus());
        String recurring = (dto.getRecurring() != null && !dto.getRecurring().isBlank()) ? dto.getRecurring() : null;
        sch.setRecurring(recurring);
        sch.setSchMemo(dto.getSchMemo());
        SchInfo saved = schInfoRepository.save(sch);
        notificationService.updateScheduleNotification(saved);
        return saved;
    }

    // 일정 삭제
    public void delete(Long schId) {
        schInfoRepository.deleteById(schId);
    }

    // 사용자의 일정 목록 조회
    public List<SchInfo> getByUserId(Long userId) {
        return schInfoRepository.findByUserInfoUserId(userId);
    }

    // 반복 업무 목록 조회
    public List<SchInfo> getRecurringByUserId(Long userId) {
        return schInfoRepository.findByUserInfoUserIdAndRecurringIsNotNull(userId);
    }

    // 일정 상세 조회
    public SchInfo getById(Long schId) {
        return schInfoRepository.findById(schId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다"));
    }
}
