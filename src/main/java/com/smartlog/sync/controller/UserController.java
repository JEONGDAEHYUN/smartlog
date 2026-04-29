package com.smartlog.sync.controller;

import com.smartlog.sync.dto.*;
import com.smartlog.sync.service.EmailVerificationService;
import com.smartlog.sync.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 사용자 인증/인가 통합 Controller
// (회원가입/로그인/이메일인증/아이디찾기/비번찾기/마이페이지/프로필/비번변경)
// 모든 입력은 DTO로 받음 (Entity 직접 노출 X)
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    // ═══════════════════════════════════════════════════════════
    // 메인 / 로그인 페이지
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // ═══════════════════════════════════════════════════════════
    // 회원가입 (3단계)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupDto", new SignupDto());
        return "auth/signup";
    }

    // Step 1: 기본정보 제출 → 인증코드 메일 발송
    @PostMapping("/signup")
    public String signupStep1(@Valid @ModelAttribute SignupDto signupDto,
                              BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            if (userService.isEmailDuplicate(signupDto.getUserEmail())) {
                model.addAttribute("error", "이미 사용 중인 이메일입니다");
                return "auth/signup";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/signup";
        }

        session.setAttribute("SIGNUP_DTO", signupDto);

        try {
            emailVerificationService.generateCode(session, signupDto.getUserEmail());
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/signup";
        }

        model.addAttribute("email", signupDto.getUserEmail());
        model.addAttribute("step", 2);
        return "auth/signup-verify";
    }

    // Step 2: 인증코드 검증 → USER_INFO INSERT
    @PostMapping("/signup/verify")
    public String signupStep2(@Valid @ModelAttribute("verifyCodeDto") VerifyCodeDto dto,
                              BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", dto.getUserEmail());
            model.addAttribute("step", 2);
            return "auth/signup-verify";
        }

        if (emailVerificationService.verifyCode(session, dto.getUserEmail(), dto.getInputCode())) {
            SignupDto signupDto = (SignupDto) session.getAttribute("SIGNUP_DTO");
            if (signupDto == null) {
                return "redirect:/signup";
            }
            try {
                userService.signup(signupDto);
                session.removeAttribute("SIGNUP_DTO");
                emailVerificationService.clearVerification(session, dto.getUserEmail());
                return "redirect:/login?signup=true";
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
                return "auth/signup";
            }
        } else {
            model.addAttribute("email", dto.getUserEmail());
            model.addAttribute("error", "인증 코드가 일치하지 않거나 만료되었습니다");
            model.addAttribute("step", 2);
            return "auth/signup-verify";
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 아이디 찾기
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/find-id")
    public String findIdPage(Model model) {
        model.addAttribute("findIdDto", new FindIdDto());
        return "auth/find-id";
    }

    @PostMapping("/find-id")
    public String findId(@Valid @ModelAttribute("findIdDto") FindIdDto dto,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/find-id";
        }
        try {
            String maskedEmail = userService.findEmail(dto.getUserName(), dto.getOrgName());
            model.addAttribute("maskedEmail", maskedEmail);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "auth/find-id";
    }

    // ═══════════════════════════════════════════════════════════
    // 비밀번호 찾기 (3단계: 이메일 → 인증코드 → 재설정)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/find-pw")
    public String findPwPage() {
        return "auth/find-pw";
    }

    // Step 1: 이메일 입력 → 인증코드 메일 발송
    @PostMapping("/find-pw/send")
    public String findPwSendCode(@Valid @ModelAttribute("emailRequestDto") EmailRequestDto dto,
                                 BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "auth/find-pw";
        }
        try {
            emailVerificationService.generateCode(session, dto.getUserEmail());
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/find-pw";
        }
        model.addAttribute("userEmail", dto.getUserEmail());
        model.addAttribute("step", 2);
        return "auth/find-pw";
    }

    // Step 2: 인증코드 검증
    @PostMapping("/find-pw/verify")
    public String findPwVerify(@Valid @ModelAttribute("verifyCodeDto") VerifyCodeDto dto,
                               BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userEmail", dto.getUserEmail());
            model.addAttribute("step", 2);
            return "auth/find-pw";
        }
        if (emailVerificationService.verifyCode(session, dto.getUserEmail(), dto.getInputCode())) {
            model.addAttribute("userEmail", dto.getUserEmail());
            model.addAttribute("step", 3);
            return "auth/find-pw";
        } else {
            model.addAttribute("userEmail", dto.getUserEmail());
            model.addAttribute("error", "인증 코드가 일치하지 않거나 만료되었습니다");
            model.addAttribute("step", 2);
            return "auth/find-pw";
        }
    }

    // Step 3: 비밀번호 재설정
    @PostMapping("/find-pw/reset")
    public String findPwReset(@Valid @ModelAttribute("passwordResetDto") PasswordResetDto dto,
                              BindingResult bindingResult,
                              Model model, HttpSession session) {
        if (!emailVerificationService.isVerified(session, dto.getUserEmail())) {
            model.addAttribute("error", "이메일 인증이 필요합니다");
            return "auth/find-pw";
        }
        if (bindingResult.hasErrors() || !dto.isPasswordMatched()) {
            model.addAttribute("error", "비밀번호가 일치하지 않거나 형식이 올바르지 않습니다");
            model.addAttribute("userEmail", dto.getUserEmail());
            model.addAttribute("step", 3);
            return "auth/find-pw";
        }
        try {
            userService.resetPassword(dto.getUserEmail(), dto.getNewPassword());
            emailVerificationService.clearVerification(session, dto.getUserEmail());
            return "redirect:/login?resetPw=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/find-pw";
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 마이페이지 (인증된 사용자)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserInfoDto user = userService.getUserDtoByEmail(userDetails.getUsername());
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "mypage/index";
    }

    // 프로필 수정
    @PostMapping("/mypage/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("profileUpdateDto") ProfileUpdateDto dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "이름과 조직명은 필수입니다");
            return "redirect:/mypage";
        }
        try {
            userService.updateProfile(userDetails.getUsername(), dto.getUserName(), dto.getOrgName());
            redirectAttributes.addFlashAttribute("success", "개인정보가 수정되었습니다");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mypage";
    }

    // 비밀번호 변경
    @PostMapping("/mypage/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid @ModelAttribute("passwordChangeDto") PasswordChangeDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !dto.isPasswordMatched()) {
            redirectAttributes.addFlashAttribute("pwError", "새 비밀번호가 일치하지 않거나 형식이 올바르지 않습니다");
            redirectAttributes.addFlashAttribute("activeTab", "password");
            return "redirect:/mypage";
        }
        try {
            userService.changePassword(userDetails.getUsername(), dto.getCurrentPassword(), dto.getNewPassword());
            return "redirect:/login?pwChanged=true";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("pwError", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "password");
            return "redirect:/mypage";
        }
    }
}
