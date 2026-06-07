package com.smartlog.sync.service;

import com.smartlog.sync.dto.SchInfoDto;
import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.dto.ScheduleStatsDto;
import com.smartlog.sync.repository.entity.SchCompletion;
import com.smartlog.sync.repository.entity.SchInfo;
import com.smartlog.sync.repository.entity.UserInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 일정 관련 비즈니스 로직 인터페이스
public interface ScheduleService {

    // 일정 등록 (NotificationService에 SchInfo 전달 필요 → 엔티티 반환)
    SchInfo create(UserInfo user, ScheduleDto dto);

    // 일정 수정 (내부용 — 엔티티 반환)
    SchInfo update(Long schId, ScheduleDto dto);

    // 일정 삭제 — 관련 알림 선삭제 + 트랜잭션
    void delete(Long schId);

    // 사용자의 일정 목록 조회 (DTO 반환)
    List<SchInfoDto> getByUserId(Long userId);

    // 반복 업무 목록 조회 (DTO 반환)
    List<SchInfoDto> getRecurringByUserId(Long userId);

    // 일정 상세 조회 (DTO 반환)
    SchInfoDto getById(Long schId);

    // 시간 충돌 검사 (DTO 반환)
    List<SchInfoDto> checkConflict(Long userId, LocalDateTime newStart, LocalDateTime newEnd, Long excludeSchId);

    // 사용자 일정의 통계 조회 (우선순위/상태별 카운트 + 완료율)
    ScheduleStatsDto getStatsByUserId(Long userId);

    // 오늘 자 일정 — 단일 일정(startDt=오늘) + 반복 일정(오늘 패턴 매치)을 모두 포함
    List<SchInfoDto> getTodaySchedules(Long userId);

    // ─── 반복 일정 완료 토글 (가상 이벤트 추적) ───
    // 해당 (schId, date) 기록이 없으면 INSERT, 있으면 DELETE
    // 반환: 토글 후 완료 상태 (true=DONE, false=PLANNED)
    boolean toggleCompletion(Long schId, LocalDate date);

    // 사용자의 모든 반복 일정 완료 기록 일괄 조회 (apiEvents 에서 N+1 회피용)
    List<SchCompletion> findCompletionsByUserId(Long userId);
}
