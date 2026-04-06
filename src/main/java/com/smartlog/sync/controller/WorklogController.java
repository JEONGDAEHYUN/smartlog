package com.smartlog.sync.controller;

import com.smartlog.sync.dto.ScheduleDto;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.entity.mongodb.Worklog;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import com.smartlog.sync.service.ScheduleService;
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
    private final UserInfoRepository userInfoRepository;

    // 원본 메모 작성 페이지
    @GetMapping("/write")
    public String writePage() {
        return "worklog/write";
    }

    // 원본 메모 저장 + AI 정제 요청
    @PostMapping("/write")
    public String write(@AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam String rawContent,
                        @RequestParam(defaultValue = "false") boolean aiRefine,
                        RedirectAttributes redirectAttributes) {
        UserInfo user = userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
        if (user == null) return "redirect:/login";

        Worklog worklog;
        if (aiRefine) {
            // 저장 + 즉시 AI 정제
            worklog = worklogService.saveAndRefine(user.getUserId(), rawContent);
        } else {
            // 임시저장 (RAW 상태)
            worklog = worklogService.saveRaw(user.getUserId(), rawContent);
        }

        redirectAttributes.addFlashAttribute("savedLogId", worklog.getLogId());
        redirectAttributes.addFlashAttribute("savedStatus", worklog.getStatus());
        return "redirect:/worklog/detail/" + worklog.getLogId();
    }

    // 업무일지 상세 (원본 vs 정제 비교)
    @GetMapping("/detail/{logId}")
    public String detail(@PathVariable String logId, Model model) {
        Worklog worklog = worklogService.getByLogId(logId);
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
        UserInfo user = userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
        if (user == null) return "redirect:/login";

        List<Worklog> worklogs = worklogService.getByUserId(user.getUserId());
        model.addAttribute("worklogs", worklogs);
        return "worklog/archive";
    }

    // 일정 확정 페이지 (AI 정제 결과 → 일정 매핑)
    @GetMapping("/confirm/{logId}")
    public String confirmPage(@PathVariable String logId, Model model) {
        Worklog worklog = worklogService.getByLogId(logId);
        model.addAttribute("worklog", worklog);

        // 기본값 세팅
        ScheduleDto dto = new ScheduleDto();
        dto.setLogId(logId);
        dto.setPriority("MID");
        dto.setStatus("PLANNED");
        model.addAttribute("scheduleDto", dto);

        return "worklog/confirm";
    }

    // 일정 확정 처리 (SCH_INFO INSERT + LOG_ID 연결)
    @PostMapping("/confirm/{logId}")
    public String confirm(@PathVariable String logId,
                          @AuthenticationPrincipal UserDetails userDetails,
                          @ModelAttribute ScheduleDto scheduleDto) {
        UserInfo user = userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
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
