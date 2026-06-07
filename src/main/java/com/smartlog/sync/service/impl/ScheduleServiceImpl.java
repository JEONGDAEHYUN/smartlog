package com.smartlog.sync.service.impl;

import com.smartlog.sync.dto.SchInfoDto;
import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.dto.ScheduleStatsDto;
import com.smartlog.sync.repository.entity.SchCompletion;
import com.smartlog.sync.repository.entity.SchInfo;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.repository.NotiInfoRepository;
import com.smartlog.sync.repository.SchCompletionRepository;
import com.smartlog.sync.repository.SchInfoRepository;
import com.smartlog.sync.service.NotificationService;
import com.smartlog.sync.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 일정 관련 비즈니스 로직 구현체
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final SchInfoRepository schInfoRepository;
    private final NotiInfoRepository notiInfoRepository;
    private final SchCompletionRepository schCompletionRepository;
    private final NotificationService notificationService;

    @Override
    public SchInfo create(UserInfo user, ScheduleDto dto) {
        String recurring = (dto.recurring() != null && !dto.recurring().isBlank()) ? dto.recurring() : null;
        SchInfo sch = SchInfo.builder()
                .userInfo(user)
                .schTitle(dto.schTitle())
                .startDt(dto.startDt())
                .endDt(dto.endDt())
                .priority(dto.priority())
                .status(dto.status())
                .logId(dto.logId())
                .recurring(recurring)
                .schMemo(dto.schMemo())
                .build();
        SchInfo saved = schInfoRepository.save(sch);
        notificationService.createScheduleNotification(saved);
        return saved;
    }

    @Override
    public SchInfo update(Long schId, ScheduleDto dto) {
        SchInfo sch = schInfoRepository.findById(schId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다"));
        String recurring = (dto.recurring() != null && !dto.recurring().isBlank()) ? dto.recurring() : null;
        sch.update(dto.schTitle(), dto.startDt(), dto.endDt(),
                dto.priority(), dto.status(), recurring, dto.schMemo());
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

    @Override
    @Transactional
    public boolean toggleCompletion(Long schId, LocalDate date) {
        Optional<SchCompletion> existing = schCompletionRepository.findBySchIdAndCompletionDate(schId, date);
        if (existing.isPresent()) {
            schCompletionRepository.delete(existing.get());
            return false;   // 완료 해제
        }
        SchCompletion entity = SchCompletion.builder()
                .schId(schId)
                .completionDate(date)
                .completedAt(LocalDateTime.now())
                .build();
        schCompletionRepository.save(entity);
        return true;        // 완료 처리
    }

    @Override
    public List<SchCompletion> findCompletionsByUserId(Long userId) {
        List<Long> schIds = schInfoRepository.findByUserInfoUserId(userId).stream()
                .map(SchInfo::getSchId)
                .toList();
        if (schIds.isEmpty()) return List.of();
        return schCompletionRepository.findBySchIdIn(schIds);
    }

    @Override
    public List<SchInfoDto> getTodaySchedules(Long userId) {
        LocalDate today = LocalDate.now();
        return getByUserId(userId).stream()
                .filter(sch -> {
                    // 단일 일정: startDt 가 오늘이면 포함
                    if (sch.recurring() == null || sch.recurring().isBlank()) {
                        return sch.startDt() != null && sch.startDt().toLocalDate().isEqual(today);
                    }
                    // 반복 일정: 등록일 이전은 제외, 오늘이 반복 패턴 매치면 포함
                    if (sch.startDt() == null || today.isBefore(sch.startDt().toLocalDate())) {
                        return false;
                    }
                    return matchesRecurringPattern(sch.recurring(), today);
                })
                .toList();
    }

    // 반복 주기 표현이 주어진 날짜와 매치되는지 — ScheduleController.apiEvents 와 동일한 규칙
    private boolean matchesRecurringPattern(String rec, LocalDate date) {
        if ("매일(주말포함)".equals(rec) || "매일".equals(rec)) {
            return true;
        }
        if ("매일(평일만)".equals(rec)) {
            java.time.DayOfWeek dow = date.getDayOfWeek();
            return dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY;
        }
        if (rec.startsWith("매주")) {
            java.time.DayOfWeek targetDay = parseDayOfWeek(rec);
            return targetDay != null && date.getDayOfWeek() == targetDay;
        }
        if ("매월 말일".equals(rec)) {
            return date.getDayOfMonth() == date.lengthOfMonth();
        }
        if (rec.startsWith("매월")) {
            return matchMonthlyRecurring(rec, date);
        }
        return false;
    }

    // 한글 요일명 ↔ DayOfWeek 매핑 (요일 추가/변경 시 이 Map 만 수정하면 됨)
    private static final java.util.Map<String, java.time.DayOfWeek> KOREAN_DAY_OF_WEEK = java.util.Map.of(
            "월요일", java.time.DayOfWeek.MONDAY,
            "화요일", java.time.DayOfWeek.TUESDAY,
            "수요일", java.time.DayOfWeek.WEDNESDAY,
            "목요일", java.time.DayOfWeek.THURSDAY,
            "금요일", java.time.DayOfWeek.FRIDAY,
            "토요일", java.time.DayOfWeek.SATURDAY,
            "일요일", java.time.DayOfWeek.SUNDAY
    );

    // "매주 화요일" 같은 표현에서 요일을 추출. 매칭 없으면 null
    private java.time.DayOfWeek parseDayOfWeek(String recurring) {
        return KOREAN_DAY_OF_WEEK.entrySet().stream()
                .filter(e -> recurring.contains(e.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    // 한글 주차명 ↔ 주차 번호 매핑 ("매월 첫째주 화요일" 같은 표현 파싱용)
    private static final java.util.Map<String, Integer> KOREAN_WEEK_OF_MONTH = java.util.Map.of(
            "첫째주", 1,
            "둘째주", 2,
            "셋째주", 3,
            "넷째주", 4
    );

    private boolean matchMonthlyRecurring(String rec, LocalDate candidate) {
        String body = rec.replace("매월 ", "");
        String[] parts = body.split(" ");
        if (parts.length != 2) return false;

        int targetWeek = KOREAN_WEEK_OF_MONTH.getOrDefault(parts[0], 0);
        java.time.DayOfWeek targetDay = parseDayOfWeek("매주 " + parts[1]);
        if (targetWeek == 0 || targetDay == null) return false;

        if (candidate.getDayOfWeek() != targetDay) return false;
        int weekOfMonth = (candidate.getDayOfMonth() - 1) / 7 + 1;
        return weekOfMonth == targetWeek;
    }
}
