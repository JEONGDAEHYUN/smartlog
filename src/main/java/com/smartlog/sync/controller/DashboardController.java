package com.smartlog.sync.controller;

import com.smartlog.sync.entity.mariadb.SchInfo;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.entity.mongodb.Worklog;
import com.smartlog.sync.repository.mariadb.NotiInfoRepository;
import com.smartlog.sync.repository.mariadb.SchInfoRepository;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import com.smartlog.sync.repository.mongodb.WorklogRepository;
import com.smartlog.sync.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

// 대시보드 Controller
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserInfoRepository userInfoRepository;
    private final SchInfoRepository schInfoRepository;
    private final WorklogRepository worklogRepository;
    private final NotiInfoRepository notiInfoRepository;
    private final WeatherService weatherService;

    @Value("${weather.api-key:}")
    private String weatherApiKey;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
        if (user == null) return "redirect:/login";

        Long userId = user.getUserId();

        // 오늘의 일정
        List<SchInfo> todaySchedules = schInfoRepository.findByUserInfoUserId(userId);
        model.addAttribute("todaySchedules", todaySchedules);

        // 최근 업무일지
        List<Worklog> recentWorklogs = worklogRepository.findByUserId(userId);
        model.addAttribute("recentWorklogs", recentWorklogs);

        // KPI 데이터
        long doneCount = todaySchedules.stream().filter(s -> "DONE".equals(s.getStatus())).count();
        long inProgressCount = todaySchedules.stream().filter(s -> "IN_PROGRESS".equals(s.getStatus())).count();
        long unreadNotiCount = notiInfoRepository.findByUserInfoUserIdAndIsRead(userId, "N").size();
        long worklogCount = recentWorklogs.size();

        model.addAttribute("doneCount", doneCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("unreadNotiCount", unreadNotiCount);
        model.addAttribute("worklogCount", worklogCount);

        // 날씨 데이터
        Map<String, Object> weather = weatherService.getSeoulWeather(weatherApiKey);
        model.addAttribute("weather", weather);

        return "dashboard";
    }
}
