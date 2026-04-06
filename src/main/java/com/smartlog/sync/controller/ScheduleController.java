package com.smartlog.sync.controller;

import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.entity.mariadb.SchInfo;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import com.smartlog.sync.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 스마트 일정 Controller
@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserInfoRepository userInfoRepository;

    // 일정 목록 (주간/월간 통합)
    @GetMapping("/list")
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        List<SchInfo> schedules = scheduleService.getByUserId(user.getUserId());

        // KPI 통계
        long totalCount = schedules.size();
        long highCount = schedules.stream().filter(s -> "HIGH".equals(s.getPriority())).count();
        long midCount = schedules.stream().filter(s -> "MID".equals(s.getPriority())).count();
        long lowCount = schedules.stream().filter(s -> "LOW".equals(s.getPriority())).count();
        long doneCount = schedules.stream().filter(s -> "DONE".equals(s.getStatus())).count();

        model.addAttribute("schedules", schedules);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("highCount", highCount);
        model.addAttribute("midCount", midCount);
        model.addAttribute("lowCount", lowCount);
        model.addAttribute("doneCount", doneCount);

        return "schedule/list";
    }

    // 일정 등록 페이지
    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("scheduleDto", new ScheduleDto());
        return "schedule/form";
    }

    // 일정 등록 처리
    @PostMapping("/create")
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute ScheduleDto scheduleDto,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "schedule/form";
        }
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        scheduleService.create(user, scheduleDto);
        return "redirect:/schedule/list";
    }

    // 일정 수정 페이지
    @GetMapping("/edit/{schId}")
    public String editPage(@PathVariable Long schId, Model model) {
        SchInfo sch = scheduleService.getById(schId);
        ScheduleDto dto = new ScheduleDto();
        dto.setSchId(sch.getSchId());
        dto.setSchTitle(sch.getSchTitle());
        dto.setStartDt(sch.getStartDt());
        dto.setEndDt(sch.getEndDt());
        dto.setPriority(sch.getPriority());
        dto.setStatus(sch.getStatus());
        dto.setLogId(sch.getLogId());
        dto.setRecurring(sch.getRecurring());
        model.addAttribute("scheduleDto", dto);
        return "schedule/form";
    }

    // 일정 수정 처리
    @PostMapping("/edit/{schId}")
    public String update(@PathVariable Long schId,
                         @Valid @ModelAttribute ScheduleDto scheduleDto,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "schedule/form";
        }
        scheduleService.update(schId, scheduleDto);
        return "redirect:/schedule/list";
    }

    // 일정 삭제
    @PostMapping("/delete/{schId}")
    public String delete(@PathVariable Long schId) {
        scheduleService.delete(schId);
        return "redirect:/schedule/list";
    }

    // 반복 업무 설정 페이지
    @GetMapping("/recurring")
    public String recurring(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        List<SchInfo> recurringList = scheduleService.getRecurringByUserId(user.getUserId());
        model.addAttribute("recurringList", recurringList);
        return "schedule/recurring";
    }

    // 월간 캘린더 페이지
    @GetMapping("/monthly")
    public String monthly(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";
        addStats(model, scheduleService.getByUserId(user.getUserId()));
        return "schedule/monthly";
    }

    // 주간 계획표 페이지
    @GetMapping("/weekly")
    public String weekly(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";
        addStats(model, scheduleService.getByUserId(user.getUserId()));
        return "schedule/weekly";
    }

    // 일정 데이터 JSON API (캘린더/주간에서 Fetch로 호출)
    @GetMapping("/api/events")
    @ResponseBody
    public List<Map<String, Object>> apiEvents(@AuthenticationPrincipal UserDetails userDetails) {
        UserInfo user = getUser(userDetails);
        if (user == null) return List.of();

        return scheduleService.getByUserId(user.getUserId()).stream().map(sch -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sch.getSchId());
            map.put("title", sch.getSchTitle());
            map.put("start", sch.getStartDt().toString());
            map.put("end", sch.getEndDt() != null ? sch.getEndDt().toString() : null);
            map.put("priority", sch.getPriority());
            map.put("status", sch.getStatus());
            return map;
        }).toList();
    }

    private void addStats(Model model, List<SchInfo> schedules) {
        model.addAttribute("totalCount", schedules.size());
        model.addAttribute("highCount", schedules.stream().filter(s -> "HIGH".equals(s.getPriority())).count());
        model.addAttribute("midCount", schedules.stream().filter(s -> "MID".equals(s.getPriority())).count());
        model.addAttribute("lowCount", schedules.stream().filter(s -> "LOW".equals(s.getPriority())).count());
        model.addAttribute("doneCount", schedules.stream().filter(s -> "DONE".equals(s.getStatus())).count());
    }

    private UserInfo getUser(UserDetails userDetails) {
        return userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
    }
}
