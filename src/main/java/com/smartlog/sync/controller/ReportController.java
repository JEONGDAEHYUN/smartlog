package com.smartlog.sync.controller;

import com.smartlog.sync.entity.mariadb.ReportInfo;
import com.smartlog.sync.entity.mariadb.SchInfo;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.repository.mariadb.SchInfoRepository;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import com.smartlog.sync.service.ReportService;
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
    private final UserInfoRepository userInfoRepository;
    private final SchInfoRepository schInfoRepository;

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

        ReportInfo report = reportService.generate(user, reportType, start, end);
        return "redirect:/report/detail/" + report.getRepId();
    }

    // 보고서 상세 (통계 포함)
    @GetMapping("/detail/{repId}")
    public String detail(@PathVariable Long repId, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        ReportInfo report = reportService.getById(repId);
        model.addAttribute("report", report);

        // 통계 데이터
        UserInfo user = getUser(userDetails);
        if (user != null) {
            List<SchInfo> schedules = schInfoRepository.findByUserInfoUserId(user.getUserId());
            long total = schedules.size();
            long high = schedules.stream().filter(s -> "HIGH".equals(s.getPriority())).count();
            long mid = schedules.stream().filter(s -> "MID".equals(s.getPriority())).count();
            long low = schedules.stream().filter(s -> "LOW".equals(s.getPriority())).count();
            long done = schedules.stream().filter(s -> "DONE".equals(s.getStatus())).count();
            long inProgress = schedules.stream().filter(s -> "IN_PROGRESS".equals(s.getStatus())).count();
            long planned = schedules.stream().filter(s -> "PLANNED".equals(s.getStatus())).count();
            double doneRate = total > 0 ? (double) done / total * 100 : 0;

            model.addAttribute("totalSch", total);
            model.addAttribute("highCount", high);
            model.addAttribute("midCount", mid);
            model.addAttribute("lowCount", low);
            model.addAttribute("doneCount", done);
            model.addAttribute("inProgressCount", inProgress);
            model.addAttribute("plannedCount", planned);
            model.addAttribute("doneRate", String.format("%.1f", doneRate));
        }
        return "report/detail";
    }

    // 문서 보관함 (목록)
    @GetMapping("/archive")
    public String archive(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        List<ReportInfo> reports = reportService.getByUserId(user.getUserId());
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
        return userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
    }
}
