package com.smartlog.sync.service;

import com.smartlog.sync.entity.mariadb.ReportInfo;
import com.smartlog.sync.entity.mariadb.SchInfo;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.entity.mongodb.Worklog;
import com.smartlog.sync.repository.mariadb.ReportInfoRepository;
import com.smartlog.sync.repository.mariadb.SchInfoRepository;
import com.smartlog.sync.repository.mongodb.WorklogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// 보고서 관련 비즈니스 로직
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportInfoRepository reportInfoRepository;
    private final SchInfoRepository schInfoRepository;
    private final WorklogRepository worklogRepository;
    private final GeminiService geminiService;

    // AI 보고서 생성 — 사용자의 일정+업무일지 데이터를 Gemini에 전달
    public ReportInfo generate(UserInfo user, String reportType) {
        Long userId = user.getUserId();

        // 일정 데이터 수집
        List<SchInfo> schedules = schInfoRepository.findByUserInfoUserId(userId);
        // 업무일지 데이터 수집
        List<Worklog> worklogs = worklogRepository.findByUserId(userId);

        // 데이터 요약 텍스트 구성
        String dataContext = buildDataContext(schedules, worklogs);

        // Gemini에 보고서 생성 요청
        String prompt = buildReportPrompt(reportType, dataContext);
        String reportContent;
        try {
            reportContent = geminiService.refineWorklog(prompt);
        } catch (Exception e) {
            log.error("보고서 생성 실패: {}", e.getMessage());
            reportContent = "보고서 생성에 실패했습니다. 다시 시도해주세요.\n원인: " + e.getMessage();
        }

        // 보고서 제목 결정
        String title = switch (reportType) {
            case "weekly" -> "주간 업무요약 보고서";
            case "monthly" -> "월간 업무요약 보고서";
            case "handover" -> "인수인계 보고서";
            default -> reportType;
        };

        // 보고서 저장
        ReportInfo report = ReportInfo.builder()
                .userInfo(user)
                .repTitle(title)
                .repCont(reportContent)
                .build();
        return reportInfoRepository.save(report);
    }

    // 사용자의 보고서 목록 조회
    public List<ReportInfo> getByUserId(Long userId) {
        return reportInfoRepository.findByUserInfoUserId(userId);
    }

    // 보고서 상세 조회
    public ReportInfo getById(Long repId) {
        return reportInfoRepository.findById(repId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다"));
    }

    // 보고서 삭제
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
            // 너무 긴 내용은 앞부분만 사용
            if (content.length() > 200) content = content.substring(0, 200) + "...";
            sb.append("- ").append(content).append(" | ").append(log.getStatus()).append("\n");
        }

        return sb.toString();
    }

    // 보고서 생성용 프롬프트
    private String buildReportPrompt(String reportType, String dataContext) {
        String typeDesc = switch (reportType) {
            case "weekly" -> "주간 업무요약 보고서";
            case "monthly" -> "월간 업무요약 보고서";
            case "handover" -> "인수인계 보고서";
            default -> reportType + " 보고서";
        };

        return """
                당신은 업무 보고서 작성 AI입니다.
                아래 데이터를 기반으로 '%s'를 작성해주세요.

                [작성 규칙]
                1. 보고서 형식: 제목, 요약, 주요 업무 내역, 통계, 특이사항, 권고사항
                2. 공식적이고 간결한 문체
                3. 데이터 기반의 구체적인 수치 포함
                4. 한국어로 작성

                %s

                [보고서 본문]
                """.formatted(typeDesc, dataContext);
    }
}
