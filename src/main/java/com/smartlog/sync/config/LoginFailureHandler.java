package com.smartlog.sync.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그인 실패 핸들러 — 5회 실패 시 계정 잠금
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final int MAX_ATTEMPTS = 5;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        String email = request.getParameter("username");

        // 실패 횟수 증가
        String key = "LOGIN_FAIL_" + email;
        Integer failCount = (Integer) session.getAttribute(key);
        failCount = (failCount == null) ? 1 : failCount + 1;
        session.setAttribute(key, failCount);

        if (failCount >= MAX_ATTEMPTS) {
            // 5회 이상 실패 → 잠금
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?locked=true&attempts=" + failCount);
        } else {
            // 일반 실패
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?error=true&attempts=" + failCount);
        }
    }
}
