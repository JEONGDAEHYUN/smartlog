package com.smartlog.sync.controller;

import com.smartlog.sync.dto.SchInfoDto;
import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.dto.WorklogDto;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.service.ScheduleService;
import com.smartlog.sync.service.UserService;
import com.smartlog.sync.service.WorklogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// AI 업무일지 Controller
@Controller
@RequestMapping("/worklog")
@RequiredArgsConstructor
public class WorklogController {

    private final WorklogService worklogService;
    private final ScheduleService scheduleService;
    private final UserService userService;

    // 원본 메모 작성 페이지 (오늘 일정 함께 표시)
    @GetMapping("/write")
    public String writePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = userService.getEntityByEmail(userDetails.getUsername());
        if (user != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            List<SchInfoDto> todaySchedules = scheduleService.getByUserId(user.getUserId()).stream()
                    .filter(s -> s.getStartDt().toLocalDate().equals(today))
                    .toList();
            model.addAttribute("todaySchedules", todaySchedules);
        }
        return "worklog/write";
    }

    // 원본 메모 저장 + AI 정제 요청
    @PostMapping("/write")
    public String write(@AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam String rawContent,
                        @RequestParam(defaultValue = "false") boolean aiRefine,
                        RedirectAttributes redirectAttributes) {
        UserInfo user = userService.getEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/login";

        com.smartlog.sync.repository.entity.Worklog worklog;
        if (aiRefine) {
            worklog = worklogService.saveAndRefine(user.getUserId(), rawContent);
        } else {
            worklog = worklogService.saveRaw(user.getUserId(), rawContent);
        }

        redirectAttributes.addFlashAttribute("savedLogId", worklog.getLogId());
        redirectAttributes.addFlashAttribute("savedStatus", worklog.getStatus());
        return "redirect:/worklog/detail/" + worklog.getLogId();
    }

    // 업무일지 상세 (원본 vs 정제 비교)
    @GetMapping("/detail/{logId}")
    public String detail(@PathVariable String logId, Model model) {
        WorklogDto worklog = worklogService.getByLogId(logId);
        model.addAttribute("worklog", worklog);
        return "worklog/detail";
    }

    // AI 재정제 요청
    @PostMapping("/refine/{logId}")
    public String refine(@PathVariable String logId) {
        worklogService.refine(logId);
        return "redirect:/worklog/detail/" + logId;
    }

    // 업무 아카이브 (목록)
    @GetMapping("/archive")
    public String archive(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = userService.getEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/login";

        List<WorklogDto> worklogs = worklogService.getByUserId(user.getUserId());
        model.addAttribute("worklogs", worklogs);
        return "worklog/archive";
    }

    // 일지 확정 페이지
    @GetMapping("/confirm/{logId}")
    public String confirmPage(@PathVariable String logId, Model model) {
        WorklogDto worklog = worklogService.getByLogId(logId);
        model.addAttribute("worklog", worklog);

        // 기본 일정 제목: "M월 d일(요일) 업무일지"
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] dayNames = {"월", "화", "수", "목", "금", "토", "일"};
        String dayOfWeek = dayNames[today.getDayOfWeek().getValue() - 1];
        String defaultTitle = today.getMonthValue() + "월 " + today.getDayOfMonth() + "일(" + dayOfWeek + ") 업무일지";

        // 빌더 패턴으로 일관된 객체 생성
        ScheduleDto dto = ScheduleDto.builder()
                .logId(logId)
                .schTitle(defaultTitle)
                .startDt(java.time.LocalDateTime.now())
                .priority("MID")
                .status("DONE")
                .build();
        model.addAttribute("scheduleDto", dto);

        return "worklog/confirm";
    }

    // 일정 확정 처리 (SCH_INFO INSERT + LOG_ID 연결)
    @PostMapping("/confirm/{logId}")
    public String confirm(@PathVariable String logId,
                          @AuthenticationPrincipal UserDetails userDetails,
                          @ModelAttribute ScheduleDto scheduleDto) {
        UserInfo user = userService.getEntityByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/login";

        scheduleDto.setLogId(logId);
        scheduleService.create(user, scheduleDto);
        return "redirect:/schedule/list";
    }

    // 업무일지 삭제
    @PostMapping("/delete/{logId}")
    public String delete(@PathVariable String logId) {
        worklogService.delete(logId);
        return "redirect:/worklog/archive";
    }
}
