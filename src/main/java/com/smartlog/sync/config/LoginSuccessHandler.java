package com.smartlog.sync.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그인 성공 핸들러 — 실패 횟수 초기화
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public LoginSuccessHandler() {
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // 로그인 성공 시 실패 횟수 초기화
        HttpSession session = request.getSession(false);
        if (session != null) {
            String email = authentication.getName();
            session.removeAttribute("LOGIN_FAIL_" + email);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
