package com.smartlog.sync.controller;

import com.smartlog.sync.dto.SignupDto;
import com.smartlog.sync.service.EmailVerificationService;
import com.smartlog.sync.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 로그인/회원가입 Controller
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    // 메인(시작) 페이지
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // ═══ 회원가입 (3단계: 기본정보 → 이메일인증 → 완료) ═══

    // Step 1: 기본정보 입력 페이지
    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupDto", new SignupDto());
        return "auth/signup";
    }

    // Step 1: 기본정보 제출 → 인증코드 발송
    @PostMapping("/signup")
    public String signupStep1(@Valid @ModelAttribute SignupDto signupDto,
                              BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        // 이메일 중복 확인
        try {
            if (userService.isEmailDuplicate(signupDto.getUserEmail())) {
                model.addAttribute("error", "이미 사용 중인 이메일입니다");
                return "auth/signup";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/signup";
        }

        // 세션에 가입 정보 임시 저장
        session.setAttribute("SIGNUP_DTO", signupDto);

        // 인증 코드 생성
        String code = emailVerificationService.generateCode(session, signupDto.getUserEmail());

        model.addAttribute("email", signupDto.getUserEmail());
        model.addAttribute("verifyCode", code); // 실제 서비스에선 이메일로 발송, 지금은 화면에 표시
        model.addAttribute("step", 2);
        return "auth/signup-verify";
    }

    // Step 2: 인증코드 확인
    @PostMapping("/signup/verify")
    public String signupStep2(@RequestParam String email, @RequestParam String inputCode,
                              Model model, HttpSession session) {
        if (emailVerificationService.verifyCode(session, email, inputCode)) {
            // 인증 성공 → 실제 회원가입 처리
            SignupDto signupDto = (SignupDto) session.getAttribute("SIGNUP_DTO");
            if (signupDto == null) {
                return "redirect:/signup";
            }

            try {
                userService.signup(signupDto);
                session.removeAttribute("SIGNUP_DTO");
                emailVerificationService.clearVerification(session, email);
                return "redirect:/login?signup=true";
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
                return "auth/signup";
            }
        } else {
            // 인증 실패
            model.addAttribute("email", email);
            model.addAttribute("error", "인증 코드가 일치하지 않거나 만료되었습니다");
            model.addAttribute("step", 2);
            return "auth/signup-verify";
        }
    }

    // ═══ 아이디 찾기 ═══

    @GetMapping("/find-id")
    public String findIdPage() {
        return "auth/find-id";
    }

    @PostMapping("/find-id")
    public String findId(@RequestParam String userName,
                         @RequestParam String orgName, Model model) {
        try {
            String maskedEmail = userService.findEmail(userName, orgName);
            model.addAttribute("maskedEmail", maskedEmail);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "auth/find-id";
    }

    // ═══ 비밀번호 찾기 (3단계: 이메일 → 인증코드 → 재설정) ═══

    @GetMapping("/find-pw")
    public String findPwPage() {
        return "auth/find-pw";
    }

    // Step 1: 이메일 입력 → 인증코드 발송
    @PostMapping("/find-pw/send")
    public String findPwSendCode(@RequestParam String userEmail, Model model, HttpSession session) {
        String code = emailVerificationService.generateCode(session, userEmail);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("verifyCode", code); // 실제 서비스에선 이메일로 발송
        model.addAttribute("step", 2);
        return "auth/find-pw";
    }

    // Step 2: 인증코드 확인
    @PostMapping("/find-pw/verify")
    public String findPwVerify(@RequestParam String userEmail, @RequestParam String inputCode,
                               Model model, HttpSession session) {
        if (emailVerificationService.verifyCode(session, userEmail, inputCode)) {
            model.addAttribute("userEmail", userEmail);
            model.addAttribute("step", 3);
            return "auth/find-pw";
        } else {
            model.addAttribute("userEmail", userEmail);
            model.addAttribute("error", "인증 코드가 일치하지 않거나 만료되었습니다");
            model.addAttribute("step", 2);
            return "auth/find-pw";
        }
    }

    // Step 3: 비밀번호 재설정
    @PostMapping("/find-pw/reset")
    public String findPwReset(@RequestParam String userEmail,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              Model model, HttpSession session) {
        if (!emailVerificationService.isVerified(session, userEmail)) {
            model.addAttribute("error", "이메일 인증이 필요합니다");
            return "auth/find-pw";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다");
            model.addAttribute("userEmail", userEmail);
            model.addAttribute("step", 3);
            return "auth/find-pw";
        }

        try {
            userService.resetPassword(userEmail, newPassword);
            emailVerificationService.clearVerification(session, userEmail);
            return "redirect:/login?resetPw=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/find-pw";
        }
    }
}
