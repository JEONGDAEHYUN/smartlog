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
    public String editPage(@PathVariable Long schId,
                           @RequestParam(required = false) String from, Model model) {
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
        dto.setSchMemo(sch.getSchMemo());
        model.addAttribute("scheduleDto", dto);
        model.addAttribute("from", from);
        return "schedule/form";
    }

    // 일정 수정 처리
    @PostMapping("/edit/{schId}")
    public String update(@PathVariable Long schId,
                         @RequestParam(required = false) String from,
                         @Valid @ModelAttribute ScheduleDto scheduleDto,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "schedule/form";
        }
        scheduleService.update(schId, scheduleDto);
        // 이전 페이지로 돌아가기
        if ("weekly".equals(from)) return "redirect:/schedule/weekly";
        if ("monthly".equals(from)) return "redirect:/schedule/monthly";
        if ("recurring".equals(from)) return "redirect:/schedule/recurring";
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

    // 일정 데이터 JSON API (캘린더/주간에서 Fetch로 호출, 반복 일정 포함)
    @GetMapping("/api/events")
    @ResponseBody
    public List<Map<String, Object>> apiEvents(@AuthenticationPrincipal UserDetails userDetails) {
        UserInfo user = getUser(userDetails);
        if (user == null) return List.of();

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();

        for (SchInfo sch : scheduleService.getByUserId(user.getUserId())) {
            // 원본 일정 추가
            result.add(toEventMap(sch, sch.getStartDt(), sch.getEndDt(), sch.getStatus()));

            // 반복 일정이면 향후 90일치 가상 이벤트 생성
            if (sch.getRecurring() != null) {
                java.time.LocalTime startTime = sch.getStartDt().toLocalTime();
                java.time.LocalTime endTime = sch.getEndDt() != null ? sch.getEndDt().toLocalTime() : null;
                java.time.LocalDate baseDate = sch.getStartDt().toLocalDate();

                for (int i = 1; i <= 90; i++) {
                    java.time.LocalDate candidate = baseDate.plusDays(i);
                    if (candidate.isBefore(today.minusDays(7))) continue; // 과거 1주 이전은 스킵

                    boolean match = false;
                    String rec = sch.getRecurring();

                    if ("매일(주말포함)".equals(rec) || "매일".equals(rec)) {
                        match = true;
                    } else if ("매일(평일만)".equals(rec)) {
                        java.time.DayOfWeek dow = candidate.getDayOfWeek();
                        match = dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY;
                    } else if (rec.startsWith("매주")) {
                        java.time.DayOfWeek targetDay = parseDayOfWeek(rec);
                        if (targetDay != null) match = candidate.getDayOfWeek() == targetDay;
                    } else if ("매월 말일".equals(rec)) {
                        match = candidate.getDayOfMonth() == candidate.lengthOfMonth();
                    } else if (rec.startsWith("매월")) {
                        // "매월 둘째주 화요일" 형태 파싱
                        match = matchMonthlyRecurring(rec, candidate);
                    }

                    if (match) {
                        java.time.LocalDateTime newStart = candidate.atTime(startTime);
                        java.time.LocalDateTime newEnd = endTime != null ? candidate.atTime(endTime) : null;
                        // 미래 날짜는 PLANNED, 과거는 DONE, 오늘은 원본 상태
                        String virtualStatus;
                        if (candidate.isAfter(today)) {
                            virtualStatus = "PLANNED";
                        } else if (candidate.isBefore(today)) {
                            virtualStatus = "DONE";
                        } else {
                            virtualStatus = sch.getStatus();
                        }
                        result.add(toEventMap(sch, newStart, newEnd, virtualStatus));
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Object> toEventMap(SchInfo sch, java.time.LocalDateTime start, java.time.LocalDateTime end, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sch.getSchId());
        map.put("title", sch.getSchTitle());
        map.put("start", start.toString());
        map.put("end", end != null ? end.toString() : null);
        map.put("priority", sch.getPriority());
        map.put("status", status);
        map.put("recurring", sch.getRecurring());
        return map;
    }

    private boolean matchMonthlyRecurring(String rec, java.time.LocalDate candidate) {
        // "매월 첫째주 월요일", "매월 둘째주 화요일" 등
        String body = rec.replace("매월 ", "");
        String[] parts = body.split(" ");
        if (parts.length != 2) return false;

        int targetWeek = switch (parts[0]) {
            case "첫째주" -> 1;
            case "둘째주" -> 2;
            case "셋째주" -> 3;
            case "넷째주" -> 4;
            default -> 0;
        };
        java.time.DayOfWeek targetDay = parseDayOfWeek("매주 " + parts[1]);
        if (targetWeek == 0 || targetDay == null) return false;

        // 해당 월의 N째주 X요일 계산
        if (candidate.getDayOfWeek() != targetDay) return false;
        int weekOfMonth = (candidate.getDayOfMonth() - 1) / 7 + 1;
        return weekOfMonth == targetWeek;
    }

    private java.time.DayOfWeek parseDayOfWeek(String recurring) {
        if (recurring.contains("월요일")) return java.time.DayOfWeek.MONDAY;
        if (recurring.contains("화요일")) return java.time.DayOfWeek.TUESDAY;
        if (recurring.contains("수요일")) return java.time.DayOfWeek.WEDNESDAY;
        if (recurring.contains("목요일")) return java.time.DayOfWeek.THURSDAY;
        if (recurring.contains("금요일")) return java.time.DayOfWeek.FRIDAY;
        if (recurring.contains("토요일")) return java.time.DayOfWeek.SATURDAY;
        if (recurring.contains("일요일")) return java.time.DayOfWeek.SUNDAY;
        return null;
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
