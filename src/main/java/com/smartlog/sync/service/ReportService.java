package com.smartlog.sync.service;

import com.smartlog.sync.dto.ReportInfoDto;
import com.smartlog.sync.repository.entity.UserInfo;

import java.time.LocalDate;
import java.util.List;

// 보고서 관련 비즈니스 로직 인터페이스
public interface ReportService {

    // AI 보고서 생성 (DTO 반환) — direct 종류는 customTitle 을 보고서 제목으로 사용
    ReportInfoDto generate(UserInfo user, String reportType, LocalDate startDate, LocalDate endDate, String customTitle);

    // 사용자의 보고서 목록 조회 (DTO 반환)
    List<ReportInfoDto> getByUserId(Long userId);

    // 보고서 상세 조회 (DTO 반환)
    ReportInfoDto getById(Long repId);

    // 보고서 삭제
    void delete(Long repId);
}
