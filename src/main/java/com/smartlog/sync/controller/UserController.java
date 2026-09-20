package com.smartlog.sync.controller;

import com.smartlog.sync.dto.*;
import com.smartlog.sync.service.EmailVerificationService;
import com.smartlog.sync.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// 사용자 인증/인가 통합 Controller
// (회원가입/로그인/이메일인증/아이디찾기/비번찾기/마이페이지/프로필/비번변경)
// 모든 입력은 DTO로 받음 (Entity 직접 노출 X)
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final SessionRegistry sessionRegistry;

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

    // 회원가입 "화면 보여주기" — GET 요청 (실제 가입 처리는 아래 @PostMapping("/signup"))
    //  - @GetMapping("/signup") : 주소창으로 /signup 에 들어오면(GET) 이 메서드 실행
    //  - Model model : 컨트롤러 → 화면(뷰)으로 데이터를 전달하는 "통로" 객체
    @GetMapping("/signup")
    public String signupPage(Model model) {
        // 빈 SignupDto를 model에 담아 화면으로 보냄
        //  - 이유: Thymeleaf 폼이 th:object="${signupDto}" 로 이 객체에 입력값을 바인딩하기 때문
        //  - SignupDto.empty() : 모든 필드 null + 권한 기본값(ROLE_USER)인 빈 인스턴스 (폼 초기 상태)
        model.addAttribute("signupDto", SignupDto.empty());
        // "auth/signup" 뷰 이름 반환 → templates/auth/signup.html 을 렌더링해서 응답
        return "auth/signup";
    }

    // Step 1: 기본정보 제출 → 인증코드 메일 발송
    // [사전질문 공통 5] Controller 주요 메서드 입력/출력 예 → 입력 @Valid SignupDto, 반환 뷰 이름(String)
    // [사전질문 JPA 11] @Valid 사용처 → SignupDto 에 적용
    @PostMapping("/signup")
    public String signupStep1(@Valid @ModelAttribute SignupDto signupDto,
                              BindingResult bindingResult, Model model, HttpSession session) {
        // [사전질문 JPA 13] 예외/검증 실패 시 메시지 반환 → BindingResult로 잡아 폼(th:errors)에 표시
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            if (userService.isEmailDuplicate(signupDto.userEmail())) {
                model.addAttribute("error", "이미 사용 중인 이메일입니다");
                return "auth/signup";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/signup";
        }
        // 임시로 입력값 보관
        session.setAttribute("SIGNUP_DTO", signupDto);
        // 인증코드 만들어 메일 발송
        try {
            emailVerificationService.generateCode(session, signupDto.userEmail());
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/signup";
        }

        model.addAttribute("email", signupDto.userEmail());
        model.addAttribute("step", 2);
        return "auth/signup-verify"; // 인증코드 입력화면 으로 넘어감
    }

    // Step 2: 인증코드 검증 → USER_INFO INSERT
    @PostMapping("/signup/verify")
    public String signupStep2(@Valid @ModelAttribute("verifyCodeDto") VerifyCodeDto dto,
                              BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", dto.userEmail());
            model.addAttribute("step", 2);
            return "auth/signup-verify";
        }

        if (emailVerificationService.verifyCode(session, dto.userEmail(), dto.inputCode())) { // 세션에 저장된 코드와 입력코드 비교
            SignupDto signupDto = (SignupDto) session.getAttribute("SIGNUP_DTO"); // 임시 저장된 사용자 입력값 꺼냄
            if (signupDto == null) {
                return "redirect:/signup";
            }
            try {
                userService.signup(signupDto);
                session.removeAttribute("SIGNUP_DTO");
                emailVerificationService.clearVerification(session, dto.userEmail());
                return "redirect:/login?signup=true";
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
                return "auth/signup";
            }
        } else {
            model.addAttribute("email", dto.userEmail());
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
        model.addAttribute("findIdDto", FindIdDto.empty());
        return "auth/find-id";
    }

    @PostMapping("/find-id")
    public String findId(@Valid @ModelAttribute("findIdDto") FindIdDto dto,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/find-id";
        }
        try {
            String maskedEmail = userService.findEmail(dto.userName(), dto.orgName());
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
    // User Enumeration 방지 — 가입 여부와 무관하게 동일한 화면으로 진행.
    // 실제 메일은 등록된 사용자에게만 발송되며, 미등록 이메일로 시도해도 동일한 응답을 받음.
    @PostMapping("/find-pw/send")
    public String findPwSendCode(@Valid @ModelAttribute("emailRequestDto") EmailRequestDto dto,
                                 BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "auth/find-pw";
        }

        if (userService.isEmailDuplicate(dto.userEmail())) {
            try {
                emailVerificationService.generateCode(session, dto.userEmail());
            } catch (IllegalStateException e) {
                // SMTP 실패는 사용자에게 노출 (등록 여부와 무관한 일반 오류)
                model.addAttribute("error", e.getMessage());
                return "auth/find-pw";
            }
        }
        // 등록 여부와 무관하게 동일한 다음 화면으로 진행
        model.addAttribute("userEmail", dto.userEmail());
        model.addAttribute("step", 2);
        return "auth/find-pw";
    }

    // Step 2: 인증코드 검증
    @PostMapping("/find-pw/verify")
    public String findPwVerify(@Valid @ModelAttribute("verifyCodeDto") VerifyCodeDto dto,
                               BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userEmail", dto.userEmail());
            model.addAttribute("step", 2);
            return "auth/find-pw";
        }
        if (emailVerificationService.verifyCode(session, dto.userEmail(), dto.inputCode())) {
            model.addAttribute("userEmail", dto.userEmail());
            model.addAttribute("step", 3);
            return "auth/find-pw";
        } else {
            model.addAttribute("userEmail", dto.userEmail());
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
        if (!emailVerificationService.isVerified(session, dto.userEmail())) {
            model.addAttribute("error", "이메일 인증이 필요합니다");
            return "auth/find-pw";
        }
        if (bindingResult.hasErrors() || !dto.isPasswordMatched()) {
            model.addAttribute("error", "비밀번호가 일치하지 않거나 형식이 올바르지 않습니다");
            model.addAttribute("userEmail", dto.userEmail());
            model.addAttribute("step", 3);
            return "auth/find-pw";
        }
        try {
            userService.resetPassword(dto.userEmail(), dto.newPassword());
            emailVerificationService.clearVerification(session, dto.userEmail());
            expireUserSessions(dto.userEmail()); // 비밀번호 변경 → 모든 활성 세션 강제 만료
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
            redirectAttributes.addFlashAttribute("error", "입력값을 확인해주세요 (이메일 형식 / 이름 / 조직명)");
            return "redirect:/mypage";
        }
        try {
            boolean emailChanged = userService.updateProfile(
                    userDetails.getUsername(), dto.userEmail(), dto.userName(), dto.orgName());

            // 이메일(로그인 ID)이 바뀌면 현재 인증 주체가 무효 → 세션 강제 만료 후 재로그인 유도
            if (emailChanged) {
                expireUserSessions(userDetails.getUsername());
                return "redirect:/login?emailChanged=true";
            }
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
            userService.changePassword(userDetails.getUsername(), dto.currentPassword(), dto.newPassword());
            expireUserSessions(userDetails.getUsername()); // 다른 디바이스 세션까지 강제 만료
            return "redirect:/login?pwChanged=true";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("pwError", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "password");
            return "redirect:/mypage";
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 내부 유틸 — 사용자별 세션 강제 만료
    // ═══════════════════════════════════════════════════════════

    // 비밀번호 변경/재설정 시 호출 — 해당 사용자의 모든 활성 세션을 즉시 만료시켜
    // 탈취된 세션이나 다른 디바이스에 남은 세션이 그대로 유지되지 않도록 함
    private void expireUserSessions(String username) {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (!(principal instanceof UserDetails)) continue;
            if (!((UserDetails) principal).getUsername().equals(username)) continue;
            for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                session.expireNow();
            }
        }
    }
}
