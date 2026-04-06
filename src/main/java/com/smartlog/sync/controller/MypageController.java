package com.smartlog.sync.controller;

import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import com.smartlog.sync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 마이페이지 Controller
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final UserInfoRepository userInfoRepository;
    private final UserService userService;

    // 마이페이지 메인
    @GetMapping
    public String mypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfo user = userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "mypage/index";
    }

    // 개인정보 수정 처리
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String userName,
                                @RequestParam String orgName,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(userDetails.getUsername(), userName, orgName);
            redirectAttributes.addFlashAttribute("success", "개인정보가 수정되었습니다");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mypage";
    }

    // 비밀번호 변경 처리
    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("pwError", "새 비밀번호가 일치하지 않습니다");
            redirectAttributes.addFlashAttribute("activeTab", "password");
            return "redirect:/mypage";
        }
        try {
            userService.changePassword(userDetails.getUsername(), currentPassword, newPassword);
            return "redirect:/login?pwChanged=true";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("pwError", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "password");
            return "redirect:/mypage";
        }
    }
}
