package com.smartlog.sync.controller;

import com.smartlog.sync.dto.ReportInfoDto;
import com.smartlog.sync.dto.ScheduleStatsDto;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.service.ReportService;
import com.smartlog.sync.service.ScheduleService;
import com.smartlog.sync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// 지능형 리포트 Controller
@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ScheduleService scheduleService;
    private final UserService userService;

    // 보고서 생성 페이지
    @GetMapping("/create")
    public String createPage() {
        return "report/create";
    }

    // AI 보고서 생성 처리
    @PostMapping("/create")
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam String reportType,
                         @RequestParam String startDate,
                         @RequestParam String endDate) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        ReportInfoDto report = reportService.generate(user, reportType, start, end);
        return "redirect:/report/detail/" + report.getRepId();
    }

    // 보고서 상세 (통계 포함)
    @GetMapping("/detail/{repId}")
    public String detail(@PathVariable Long repId, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        ReportInfoDto report = reportService.getById(repId);
        model.addAttribute("report", report);

        // 통계 데이터 (ScheduleService에서 일원화된 통계 계산)
        UserInfo user = getUser(userDetails);
        if (user != null) {
            ScheduleStatsDto stats = scheduleService.getStatsByUserId(user.getUserId());
            model.addAttribute("totalSch", stats.getTotalCount());
            model.addAttribute("highCount", stats.getHighCount());
            model.addAttribute("midCount", stats.getMidCount());
            model.addAttribute("lowCount", stats.getLowCount());
            model.addAttribute("doneCount", stats.getDoneCount());
            model.addAttribute("inProgressCount", stats.getInProgressCount());
            model.addAttribute("plannedCount", stats.getPlannedCount());
            model.addAttribute("doneRate", stats.getDoneRate());
        }
        return "report/detail";
    }

    // 문서 보관함 (목록)
    @GetMapping("/archive")
    public String archive(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        List<ReportInfoDto> reports = reportService.getByUserId(user.getUserId());
        model.addAttribute("reports", reports);
        return "report/archive";
    }

    // 보고서 삭제
    @PostMapping("/delete/{repId}")
    public String delete(@PathVariable Long repId) {
        reportService.delete(repId);
        return "redirect:/report/archive";
    }

    private UserInfo getUser(UserDetails userDetails) {
        return userService.getEntityByEmail(userDetails.getUsername());
    }
}
