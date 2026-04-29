package com.smartlog.sync.service.impl;

import com.smartlog.sync.dto.SchInfoDto;
import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.dto.ScheduleStatsDto;
import com.smartlog.sync.repository.entity.SchInfo;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.repository.NotiInfoRepository;
import com.smartlog.sync.repository.SchInfoRepository;
import com.smartlog.sync.service.NotificationService;
import com.smartlog.sync.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 일정 관련 비즈니스 로직 구현체
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final SchInfoRepository schInfoRepository;
    private final NotiInfoRepository notiInfoRepository;
    private final NotificationService notificationService;

    @Override
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

    @Override
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

    @Override
    @Transactional
    public void delete(Long schId) {
        notiInfoRepository.deleteBySchInfoSchId(schId);
        schInfoRepository.deleteById(schId);
    }

    @Override
    public List<SchInfoDto> getByUserId(Long userId) {
        return SchInfoDto.fromList(schInfoRepository.findByUserInfoUserId(userId));
    }

    @Override
    public List<SchInfoDto> getRecurringByUserId(Long userId) {
        return SchInfoDto.fromList(schInfoRepository.findByUserInfoUserIdAndRecurringIsNotNull(userId));
    }

    @Override
    public SchInfoDto getById(Long schId) {
        SchInfo sch = schInfoRepository.findById(schId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다"));
        return SchInfoDto.from(sch);
    }

    @Override
    public List<SchInfoDto> checkConflict(Long userId, LocalDateTime newStart, LocalDateTime newEnd, Long excludeSchId) {
        if (newStart == null || newEnd == null) return List.of();
        if (!newEnd.isAfter(newStart)) return List.of();
        List<SchInfo> conflicts = schInfoRepository.findConflicts(userId, newStart, newEnd);
        if (excludeSchId != null) {
            conflicts = conflicts.stream()
                    .filter(s -> !excludeSchId.equals(s.getSchId()))
                    .toList();
        }
        return SchInfoDto.fromList(conflicts);
    }

    @Override
    public ScheduleStatsDto getStatsByUserId(Long userId) {
        return ScheduleStatsDto.from(getByUserId(userId));
    }
}
