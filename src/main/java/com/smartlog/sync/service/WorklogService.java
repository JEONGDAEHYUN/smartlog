package com.smartlog.sync.service;

import com.smartlog.sync.dto.WorklogDto;
import com.smartlog.sync.repository.entity.Worklog;

import java.util.List;

// 업무일지 비즈니스 로직 인터페이스
public interface WorklogService {

    // 원본 메모 저장 (RAW 상태) — 내부용 엔티티 반환
    Worklog saveRaw(Long userId, String rawContent);

    // AI 정제 요청 (RAW → PROCESSING → SUCCESS/FAILED) — 내부용 엔티티 반환
    Worklog refine(String logId);

    // 원본 메모 저장 + 즉시 AI 정제 — 내부용 엔티티 반환
    Worklog saveAndRefine(Long userId, String rawContent);

    // 사용자의 업무일지 목록 조회 (DTO 반환)
    List<WorklogDto> getByUserId(Long userId);

    // 업무일지 상세 조회 (DTO 반환)
    WorklogDto getByLogId(String logId);

    // 업무일지 삭제
    void delete(String logId);
}
