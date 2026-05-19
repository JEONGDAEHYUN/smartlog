package com.smartlog.sync.config;

import com.smartlog.sync.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그인 성공 핸들러 — 실패 카운터 / 잠금 상태 초기화 (DB)
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;

    public LoginSuccessHandler(UserService userService) {
        this.userService = userService;
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // DB의 failCount, lockedUntil 초기화
        userService.resetLoginFailures(authentication.getName());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
