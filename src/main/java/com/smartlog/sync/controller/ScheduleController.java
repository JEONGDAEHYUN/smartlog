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
    private final com.smartlog.sync.service.WorklogService worklogService;

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
        model.addAttribute("scheduleDto", ScheduleDto.empty());
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
                .schId(sch.schId())
                .schTitle(sch.schTitle())
                .startDt(sch.startDt())
                .endDt(sch.endDt())
                .priority(sch.priority())
                .status(sch.status())
                .logId(sch.logId())
                .recurring(sch.recurring())
                .schMemo(sch.schMemo())
                .build();
        model.addAttribute("scheduleDto", dto);
        model.addAttribute("from", from);

        // 업무일지에서 변환된 일정이면 원본 업무일지를 form 에 함께 전달 (컨텍스트 가시화)
        if (sch.logId() != null && !sch.logId().isBlank()) {
            try {
                model.addAttribute("linkedWorklog", worklogService.getByLogId(sch.logId()));
            } catch (Exception ignore) {
                // 업무일지가 삭제된 일정 — 카드 표시 안 함
            }
        }

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

        // 사용자의 모든 반복 일정 완료 기록 일괄 조회 → "schId:yyyy-MM-dd" Set 으로 변환 (apiEvents 내 매칭용)
        java.util.Set<String> completionKeys = scheduleService.findCompletionsByUserId(user.getUserId()).stream()
                .map(c -> c.getSchId() + ":" + c.getCompletionDate())
                .collect(java.util.stream.Collectors.toSet());

        for (SchInfoDto sch : scheduleService.getByUserId(user.getUserId())) {
            // 원본 일정 추가
            // — 반복 일정: 원본 startDt 날짜의 완료 기록 기반으로 표시
            // — 단일 일정: 원본 status 그대로
            String originalStatus = (sch.recurring() != null)
                    ? (completionKeys.contains(sch.schId() + ":" + sch.startDt().toLocalDate()) ? "DONE" : "PLANNED")
                    : sch.status();
            result.add(toEventMap(sch, sch.startDt(), sch.endDt(), originalStatus));

            // 반복 일정이면 향후 90일치 가상 이벤트 생성
            if (sch.recurring() != null) {
                java.time.LocalTime startTime = sch.startDt().toLocalTime();
                java.time.LocalTime endTime = sch.endDt() != null ? sch.endDt().toLocalTime() : null;
                java.time.LocalDate baseDate = sch.startDt().toLocalDate();
                java.time.LocalDate today = java.time.LocalDate.now();

                for (int i = 1; i <= 90; i++) {
                    java.time.LocalDate candidate = baseDate.plusDays(i);
                    if (candidate.isBefore(today.minusDays(7))) continue; // 과거 1주 이전은 스킵

                    boolean match = false;
                    String rec = sch.recurring();

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
                        // 가상 이벤트 status — 사용자가 그 날짜에 완료 표시한 경우에만 DONE
                        String key = sch.schId() + ":" + candidate;
                        String virtualStatus = completionKeys.contains(key) ? "DONE" : "PLANNED";
                        result.add(toEventMap(sch, newStart, newEnd, virtualStatus));
                    }
                }
            }
        }
        return result;
    }

    // 반복 일정 완료 토글 API (캘린더 가상 이벤트 클릭 시 호출)
    @PostMapping("/api/complete")
    @ResponseBody
    public Map<String, Object> toggleComplete(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestParam Long schId,
                                              @RequestParam String date) {
        UserInfo user = getUser(userDetails);
        if (user == null) return Map.of("success", false, "error", "unauthorized");

        java.time.LocalDate parsedDate;
        try {
            parsedDate = java.time.LocalDate.parse(date);
        } catch (Exception e) {
            return Map.of("success", false, "error", "invalid date");
        }

        boolean completed = scheduleService.toggleCompletion(schId, parsedDate);
        return Map.of("success", true, "completed", completed);
    }

    private Map<String, Object> toEventMap(SchInfoDto sch, java.time.LocalDateTime start, java.time.LocalDateTime end, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sch.schId());
        map.put("title", sch.schTitle());
        map.put("start", start.toString());
        map.put("end", end != null ? end.toString() : null);
        map.put("priority", sch.priority());
        map.put("status", status);
        map.put("recurring", sch.recurring());
        return map;
    }

    // 한글 주차명 ↔ 주차 번호 매핑 ("매월 첫째주 화요일" 같은 표현 파싱용)
    private static final java.util.Map<String, Integer> KOREAN_WEEK_OF_MONTH = java.util.Map.of(
            "첫째주", 1,
            "둘째주", 2,
            "셋째주", 3,
            "넷째주", 4
    );

    private boolean matchMonthlyRecurring(String rec, java.time.LocalDate candidate) {
        String body = rec.replace("매월 ", "");
        String[] parts = body.split(" ");
        if (parts.length != 2) return false;

        int targetWeek = KOREAN_WEEK_OF_MONTH.getOrDefault(parts[0], 0);
        java.time.DayOfWeek targetDay = parseDayOfWeek("매주 " + parts[1]);
        if (targetWeek == 0 || targetDay == null) return false;

        if (candidate.getDayOfWeek() != targetDay) return false;
        int weekOfMonth = (candidate.getDayOfMonth() - 1) / 7 + 1;
        return weekOfMonth == targetWeek;
    }

    // 한글 요일명 ↔ DayOfWeek 매핑 (요일 추가/변경 시 이 Map 만 수정하면 됨)
    private static final java.util.Map<String, java.time.DayOfWeek> KOREAN_DAY_OF_WEEK = java.util.Map.of(
            "월요일", java.time.DayOfWeek.MONDAY,
            "화요일", java.time.DayOfWeek.TUESDAY,
            "수요일", java.time.DayOfWeek.WEDNESDAY,
            "목요일", java.time.DayOfWeek.THURSDAY,
            "금요일", java.time.DayOfWeek.FRIDAY,
            "토요일", java.time.DayOfWeek.SATURDAY,
            "일요일", java.time.DayOfWeek.SUNDAY
    );

    // "매주 화요일" 같은 표현에서 요일을 추출. 매칭 없으면 null
    private java.time.DayOfWeek parseDayOfWeek(String recurring) {
        return KOREAN_DAY_OF_WEEK.entrySet().stream()
                .filter(e -> recurring.contains(e.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
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
            m.put("schId", s.schId());
            m.put("title", s.schTitle());
            m.put("startDt", s.startDt().format(fmt));
            m.put("endDt", s.endDt() != null ? s.endDt().format(fmt) : "");
            m.put("priority", s.priority());
            return m;
        }).toList();
    }

    // 통계 데이터를 model에 추가 (ScheduleStatsDto 활용)
    private void addStats(Model model, List<SchInfoDto> schedules) {
        addStatsToModel(model, ScheduleStatsDto.from(schedules));
    }

    private void addStatsToModel(Model model, ScheduleStatsDto stats) {
        model.addAttribute("totalCount", stats.totalCount());
        model.addAttribute("highCount", stats.highCount());
        model.addAttribute("midCount", stats.midCount());
        model.addAttribute("lowCount", stats.lowCount());
        model.addAttribute("doneCount", stats.doneCount());
    }

    private UserInfo getUser(UserDetails userDetails) {
        return userService.getEntityByEmail(userDetails.getUsername());
    }
}
