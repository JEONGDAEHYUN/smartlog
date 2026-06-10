package com.smartlog.sync.service;

import com.smartlog.sync.dto.ReportInfoDto;
import com.smartlog.sync.repository.entity.UserInfo;

import java.time.LocalDate;
import java.util.List;

// 보고서 관련 비즈니스 로직 인터페이스
public interface ReportService {

    // AI 보고서 미리보기 생성 — Gemini 호출만, DB INSERT 하지 않음 (repId=null)
    // 사용자가 "저장" 버튼을 명시적으로 누르기 전까지는 DB 에 들어가지 않는다
    ReportInfoDto generatePreview(UserInfo user, String reportType, LocalDate startDate, LocalDate endDate, String customTitle);

    // 미리보기 결과를 DB 에 영구 저장 — repId 가 있는 DTO 반환
    ReportInfoDto savePreview(UserInfo user, ReportInfoDto preview);

    // 사용자의 보고서 목록 조회 (DTO 반환)
    List<ReportInfoDto> getByUserId(Long userId);

    // 보고서 상세 조회 (DTO 반환)
    ReportInfoDto getById(Long repId);

    // 보고서 삭제
    void delete(Long repId);
}
