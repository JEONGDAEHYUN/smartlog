package com.smartlog.sync.controller;

import com.smartlog.sync.dto.CurrentWeatherDto;
import com.smartlog.sync.dto.SchInfoDto;
import com.smartlog.sync.dto.WorklogDto;
import com.smartlog.sync.repository.NotiInfoRepository;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.service.ScheduleService;
import com.smartlog.sync.service.UserService;
import com.smartlog.sync.service.WeatherService;
import com.smartlog.sync.service.WorklogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

// 대시보드 Controller
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final NotiInfoRepository notiInfoRepository;
    private final ScheduleService scheduleService;
    private final WorklogService worklogService;
    private final WeatherService weatherService;

    @Value("${weather.api-key:}")
    private String weatherApiKey;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = userService.getEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/login";

        Long userId = user.getUserId();

        // 오늘의 일정 (Service에서 DTO로 받음)
        List<SchInfoDto> todaySchedules = scheduleService.getByUserId(userId);
        model.addAttribute("todaySchedules", todaySchedules);

        // 최근 업무일지 (Service에서 DTO로 받음)
        List<WorklogDto> recentWorklogs = worklogService.getByUserId(userId);
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
        CurrentWeatherDto weather = weatherService.getSeoulWeather(weatherApiKey);
        model.addAttribute("weather", weather);

        return "dashboard";
    }
}
