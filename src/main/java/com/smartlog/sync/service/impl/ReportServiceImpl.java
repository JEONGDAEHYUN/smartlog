package com.smartlog.sync.service.impl;

import com.smartlog.sync.dto.ReportInfoDto;
import com.smartlog.sync.dto.ReportType;
import com.smartlog.sync.repository.entity.ReportInfo;
import com.smartlog.sync.repository.entity.SchInfo;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.repository.entity.Worklog;
import com.smartlog.sync.repository.ReportInfoRepository;
import com.smartlog.sync.repository.SchInfoRepository;
import com.smartlog.sync.repository.WorklogRepository;
import com.smartlog.sync.service.GeminiService;
import com.smartlog.sync.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// 보고서 관련 비즈니스 로직 구현체
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportInfoRepository reportInfoRepository;
    private final SchInfoRepository schInfoRepository;
    private final WorklogRepository worklogRepository;
    private final GeminiService geminiService;

    @Override
    public ReportInfoDto generate(UserInfo user, String reportType, LocalDate startDate, LocalDate endDate) {
        Long userId = user.getUserId();

        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.atTime(LocalTime.MAX);

        List<SchInfo> schedules = schInfoRepository.findByUserInfoUserIdAndStartDtBetween(userId, startDt, endDt);
        List<Worklog> worklogs = worklogRepository.findByUserIdAndCreatedAtBetween(userId, startDt, endDt);

        String dataContext = buildDataContext(schedules, worklogs);

        String periodInfo = startDate + " ~ " + endDate;
        String prompt = buildReportPrompt(reportType, dataContext, periodInfo);
        String reportContent;
        try {
            reportContent = geminiService.refineWorklog(prompt);
        } catch (Exception e) {
            log.error("보고서 생성 실패: {}", e.getMessage());
            reportContent = "보고서 생성에 실패했습니다. 다시 시도해주세요.\n원인: " + e.getMessage();
        }

        String title = ReportType.toTitle(reportType);

        ReportInfo report = ReportInfo.builder()
                .userInfo(user)
                .repTitle(title)
                .repCont(reportContent)
                .build();
        return ReportInfoDto.from(reportInfoRepository.save(report));
    }

    @Override
    public List<ReportInfoDto> getByUserId(Long userId) {
        return ReportInfoDto.fromList(reportInfoRepository.findByUserInfoUserId(userId));
    }

    @Override
    public ReportInfoDto getById(Long repId) {
        ReportInfo report = reportInfoRepository.findById(repId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다"));
        return ReportInfoDto.from(report);
    }

    @Override
    public void delete(Long repId) {
        reportInfoRepository.deleteById(repId);
    }

    // 일정+업무일지 데이터를 텍스트로 구성
    private String buildDataContext(List<SchInfo> schedules, List<Worklog> worklogs) {
        StringBuilder sb = new StringBuilder();

        sb.append("[일정 데이터 - 총 ").append(schedules.size()).append("건]\n");
        for (SchInfo sch : schedules) {
            sb.append("- ").append(sch.getSchTitle())
                    .append(" | ").append(sch.getPriority())
                    .append(" | ").append(sch.getStatus())
                    .append(" | ").append(sch.getStartDt()).append("\n");
        }

        sb.append("\n[업무일지 데이터 - 총 ").append(worklogs.size()).append("건]\n");
        for (Worklog log : worklogs) {
            String content = log.getRefinedContent() != null ? log.getRefinedContent() : log.getRawContent();
            if (content.length() > 200) content = content.substring(0, 200) + "...";
            sb.append("- ").append(content).append(" | ").append(log.getStatus()).append("\n");
        }

        return sb.toString();
    }

    // 보고서 생성용 프롬프트
    private String buildReportPrompt(String reportType, String dataContext, String periodInfo) {
        String typeDesc = ReportType.toTitle(reportType);

        return """
                당신은 업무 보고서 작성 전문가입니다.
                아래 [업무 데이터]만을 기반으로 '%s'를 작성하세요.

                [보고서 기간] %s

                [필수 규칙]
                1. 보고서 형식: 제목, 요약, 주요 업무 내역, 통계, 특이사항, 권고사항
                2. 공식적이고 간결한 문체로 작성
                3. 데이터 기반의 구체적인 수치 포함
                4. 한국어로 작성
                5. 보고서 기간을 제목과 요약에 명시
                6. 보고서 본문만 출력하고, 이 프롬프트나 원본 데이터 형식에 대한 언급은 절대 하지 마세요
                7. 비고란에 데이터 출처나 AI 관련 메타 정보를 언급하지 마세요

                [업무 데이터]
                %s

                [보고서 본문 시작]
                """.formatted(typeDesc, periodInfo, dataContext);
    }
}
