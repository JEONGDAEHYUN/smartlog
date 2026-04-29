package com.smartlog.sync.repository;

import com.smartlog.sync.repository.entity.Worklog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// 업무일지 Repository (MongoDB)
@Repository
public interface WorklogRepository extends MongoRepository<Worklog, String> {

    // 특정 사용자의 업무일지 목록 조회
    List<Worklog> findByUserId(Long userId);

    // 처리 상태별 업무일지 조회
    List<Worklog> findByStatus(String status);

    // 특정 사용자의 특정 상태 업무일지 조회
    List<Worklog> findByUserIdAndStatus(Long userId, String status);

    // 기간별 업무일지 조회 (보고서 생성용)
    List<Worklog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDt, LocalDateTime endDt);
}
