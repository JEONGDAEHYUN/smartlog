package com.smartlog.sync.controller;

import com.smartlog.sync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 관리자 전용 Controller — SecurityConfig에서 ROLE_ADMIN 만 접근 허용
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    // 회원 목록 페이지
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    // 계정 잠금/해제 토글
    @PostMapping("/users/{userId}/lock")
    public String toggleLock(@PathVariable Long userId,
                             @AuthenticationPrincipal UserDetails me,
                             RedirectAttributes redirectAttributes) {
        // 자기 자신 잠금 방지
        if (userService.getUserDtoByEmail(me.getUsername()).userId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "자신의 계정은 잠금할 수 없습니다.");
            return "redirect:/admin/users";
        }
        try {
            userService.adminToggleLock(userId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // 권한 전환 (ROLE_USER ↔ ROLE_ADMIN)
    @PostMapping("/users/{userId}/role")
    public String toggleRole(@PathVariable Long userId,
                             @AuthenticationPrincipal UserDetails me,
                             RedirectAttributes redirectAttributes) {
        // 자기 자신 권한 변경 방지
        if (userService.getUserDtoByEmail(me.getUsername()).userId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "자신의 권한은 변경할 수 없습니다.");
            return "redirect:/admin/users";
        }
        try {
            userService.adminToggleRole(userId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}