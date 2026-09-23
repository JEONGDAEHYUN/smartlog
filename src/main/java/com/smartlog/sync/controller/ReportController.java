package com.smartlog.sync.controller;

import com.smartlog.sync.dto.FileInfoDto;
import com.smartlog.sync.dto.ReportInfoDto;
import com.smartlog.sync.dto.ScheduleStatsDto;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.service.FileService;
import com.smartlog.sync.service.ReportService;
import com.smartlog.sync.service.ScheduleService;
import com.smartlog.sync.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

// 지능형 리포트 Controller
@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    // 미리보기 결과를 임시 보관하는 세션 키
    private static final String PREVIEW_SESSION_KEY = "REPORT_PREVIEW";

    private final ReportService reportService;
    private final ScheduleService scheduleService;
    private final UserService userService;
    private final FileService fileService;

    // 보고서 생성 페이지
    @GetMapping("/create")
    public String createPage() {
        return "report/create";
    }

    // AI 보고서 미리보기 생성 — Gemini 호출 후 DB 저장 없이 세션에 임시 보관
    // 사용자가 "보관함에 저장" 버튼을 명시적으로 누르기 전까지는 DB INSERT 하지 않음
    @PostMapping("/create")
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam String reportType,
                         @RequestParam String startDate,
                         @RequestParam String endDate,
                         @RequestParam(required = false) String customTitle,
                         HttpSession session) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        ReportInfoDto preview = reportService.generatePreview(user, reportType, start, end, customTitle);
        session.setAttribute(PREVIEW_SESSION_KEY, preview);
        return "redirect:/report/preview";
    }

    // 미리보기 화면 — 세션에 미리보기 결과가 있으면 보여주고, 없으면 생성 페이지로
    @GetMapping("/preview")
    public String previewPage(HttpSession session, Model model) {
        Object previewObj = session.getAttribute(PREVIEW_SESSION_KEY);
        if (!(previewObj instanceof ReportInfoDto preview)) {
            return "redirect:/report/create";
        }
        model.addAttribute("preview", preview);
        return "report/preview";
    }

    // 미리보기 → 실제 저장 (REPORT_INFO INSERT) 후 상세 페이지로 이동
    @PostMapping("/save")
    public String savePreview(@AuthenticationPrincipal UserDetails userDetails, HttpSession session,
                              RedirectAttributes redirectAttributes) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        Object previewObj = session.getAttribute(PREVIEW_SESSION_KEY);
        if (!(previewObj instanceof ReportInfoDto preview)) {
            return "redirect:/report/create";
        }

        ReportInfoDto saved = reportService.savePreview(user, preview);
        session.removeAttribute(PREVIEW_SESSION_KEY);     // 저장 후 세션 정리
        redirectAttributes.addFlashAttribute("justSaved", true);  // 상세 화면에서 "저장 완료" 모달 표시용
        return "redirect:/report/detail/" + saved.repId();
    }

    // 미리보기 폐기 — 저장하지 않고 세션 비운 후 생성 페이지로 되돌아감
    @PostMapping("/discard")
    public String discardPreview(HttpSession session) {
        session.removeAttribute(PREVIEW_SESSION_KEY);
        return "redirect:/report/create";
    }

    // 보고서 상세 (통계 + 첨부파일 목록 포함)
    @GetMapping("/detail/{repId}")
    public String detail(@PathVariable Long repId, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        ReportInfoDto report = reportService.getById(repId);
        model.addAttribute("report", report);
        model.addAttribute("files", fileService.getByRepId(repId));

        UserInfo user = getUser(userDetails);
        if (user != null) {
            ScheduleStatsDto stats = scheduleService.getStatsByUserId(user.getUserId());
            model.addAttribute("totalSch", stats.totalCount());
            model.addAttribute("highCount", stats.highCount());
            model.addAttribute("midCount", stats.midCount());
            model.addAttribute("lowCount", stats.lowCount());
            model.addAttribute("doneCount", stats.doneCount());
            model.addAttribute("inProgressCount", stats.inProgressCount());
            model.addAttribute("plannedCount", stats.plannedCount());
            model.addAttribute("doneRate", stats.doneRate());
        }
        return "report/detail";
    }

    // ─── 첨부파일 ──────────────────────────────────────────────

    // 파일 업로드
    @PostMapping("/detail/{repId}/upload")
    public String uploadFile(@AuthenticationPrincipal UserDetails userDetails,
                             @PathVariable Long repId,
                             @RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";
        try {
            fileService.upload(repId, user.getUserId(), file);
            redirectAttributes.addFlashAttribute("fileMsg", "파일이 업로드되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("fileError", e.getMessage());
        }
        return "redirect:/report/detail/" + repId;
    }

    // 파일 다운로드 (이미지 → 인라인 미리보기, PDF → 다운로드)
    @GetMapping("/file/{fileId}/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable Long fileId) {
        UserInfo user = getUser(userDetails);
        if (user == null) return ResponseEntity.status(401).build();
        try {
            FileInfoDto fileDto = fileService.getById(fileId);
            Resource resource = fileService.loadAsResource(fileId, user.getUserId());
            String disposition = fileDto.isImage() ? "inline" : "attachment";
            String encodedName = URLEncoder.encode(fileDto.originalName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileDto.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename*=UTF-8''" + encodedName)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 파일 삭제
    @PostMapping("/file/{fileId}/delete")
    public String deleteFile(@AuthenticationPrincipal UserDetails userDetails,
                             @PathVariable Long fileId,
                             @RequestParam Long repId,
                             RedirectAttributes redirectAttributes) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";
        try {
            fileService.delete(fileId, user.getUserId());
            redirectAttributes.addFlashAttribute("fileMsg", "파일이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("fileError", "파일 삭제 실패: " + e.getMessage());
        }
        return "redirect:/report/detail/" + repId;
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

    // 보고서 삭제 (단건)
    @PostMapping("/delete/{repId}")
    public String delete(@PathVariable Long repId) {
        reportService.delete(repId);
        return "redirect:/report/archive";
    }

    // 보고서 다중 삭제 — 문서 보관함의 체크박스 선택 항목 일괄 처리
    @PostMapping("/delete-multiple")
    public String deleteMultiple(@RequestParam(value = "repIds", required = false) List<Long> repIds) {
        if (repIds != null) {
            for (Long id : repIds) reportService.delete(id);
        }
        return "redirect:/report/archive";
    }

    private UserInfo getUser(UserDetails userDetails) {
        return userService.getEntityByEmail(userDetails.getUsername());
    }
}
