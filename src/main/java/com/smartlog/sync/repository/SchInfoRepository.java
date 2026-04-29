package com.smartlog.sync.repository;

import com.smartlog.sync.repository.entity.SchInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// 일정정보 Repository (MariaDB)
@Repository
public interface SchInfoRepository extends JpaRepository<SchInfo, Long> {

    // 특정 사용자의 일정 목록 조회
    List<SchInfo> findByUserInfoUserId(Long userId);

    // MongoDB WORKLOG 연결용 LOG_ID로 일정 조회
    SchInfo findByLogId(String logId);

    // 반복 업무 목록 조회 (recurring이 NULL이 아닌 것)
    List<SchInfo> findByUserInfoUserIdAndRecurringIsNotNull(Long userId);

    // 기간별 일정 조회 (보고서 생성용)
    List<SchInfo> findByUserInfoUserIdAndStartDtBetween(Long userId, LocalDateTime startDt, LocalDateTime endDt);

    // 시간 충돌 일정 조회 — 두 구간이 겹치는 조건: existing.startDt < newEnd AND existing.endDt > newStart
    // 종료시간(endDt)이 NULL인 일정은 "진행중"으로 간주하여 충돌 판정에서 제외
    @Query("SELECT s FROM SchInfo s WHERE s.userInfo.userId = :userId " +
            "AND s.endDt IS NOT NULL " +
            "AND s.startDt < :newEnd " +
            "AND s.endDt > :newStart")
    List<SchInfo> findConflicts(@Param("userId") Long userId,
                                @Param("newStart") LocalDateTime newStart,
                                @Param("newEnd") LocalDateTime newEnd);
}
