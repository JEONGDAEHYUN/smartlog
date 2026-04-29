package com.smartlog.sync.controller;

import com.smartlog.sync.dto.SchInfoDto;
import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.dto.ScheduleStatsDto;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.service.ScheduleService;
import com.smartlog.sync.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 스마트 일정 Controller
@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;

    // 일정 목록 (주간/월간 통합)
    @GetMapping("/list")
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        List<SchInfoDto> schedules = scheduleService.getByUserId(user.getUserId());
        model.addAttribute("schedules", schedules);
        addStatsToModel(model, ScheduleStatsDto.from(schedules));
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
        SchInfoDto sch = scheduleService.getById(schId);
        ScheduleDto dto = ScheduleDto.builder()
                .schId(sch.getSchId())
                .schTitle(sch.getSchTitle())
                .startDt(sch.getStartDt())
                .endDt(sch.getEndDt())
                .priority(sch.getPriority())
                .status(sch.getStatus())
                .logId(sch.getLogId())
                .recurring(sch.getRecurring())
                .schMemo(sch.getSchMemo())
                .build();
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

        List<SchInfoDto> recurringList = scheduleService.getRecurringByUserId(user.getUserId());
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

        for (SchInfoDto sch : scheduleService.getByUserId(user.getUserId())) {
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

    private Map<String, Object> toEventMap(SchInfoDto sch, java.time.LocalDateTime start, java.time.LocalDateTime end, String status) {
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

    // 충돌 검사 API — 등록/수정 전 모달 경고용
    @GetMapping("/api/check-conflict")
    @ResponseBody
    public List<Map<String, Object>> checkConflict(@AuthenticationPrincipal UserDetails userDetails,
                                                   @RequestParam String startDt,
                                                   @RequestParam String endDt,
                                                   @RequestParam(required = false) Long excludeSchId) {
        UserInfo user = getUser(userDetails);
        if (user == null) return List.of();

        java.time.LocalDateTime start;
        java.time.LocalDateTime end;
        try {
            start = java.time.LocalDateTime.parse(startDt);
            end = java.time.LocalDateTime.parse(endDt);
        } catch (Exception e) {
            return List.of();
        }

        List<SchInfoDto> conflicts = scheduleService.checkConflict(user.getUserId(), start, end, excludeSchId);
        log.info("[충돌검사] userId={}, range={}~{}, excludeSchId={}, 결과={}건",
                user.getUserId(), start, end, excludeSchId, conflicts.size());
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm");
        return conflicts.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("schId", s.getSchId());
            m.put("title", s.getSchTitle());
            m.put("startDt", s.getStartDt().format(fmt));
            m.put("endDt", s.getEndDt() != null ? s.getEndDt().format(fmt) : "");
            m.put("priority", s.getPriority());
            return m;
        }).toList();
    }

    // 통계 데이터를 model에 추가 (ScheduleStatsDto 활용)
    private void addStats(Model model, List<SchInfoDto> schedules) {
        addStatsToModel(model, ScheduleStatsDto.from(schedules));
    }

    private void addStatsToModel(Model model, ScheduleStatsDto stats) {
        model.addAttribute("totalCount", stats.getTotalCount());
        model.addAttribute("highCount", stats.getHighCount());
        model.addAttribute("midCount", stats.getMidCount());
        model.addAttribute("lowCount", stats.getLowCount());
        model.addAttribute("doneCount", stats.getDoneCount());
    }

    private UserInfo getUser(UserDetails userDetails) {
        return userService.getEntityByEmail(userDetails.getUsername());
    }
}
