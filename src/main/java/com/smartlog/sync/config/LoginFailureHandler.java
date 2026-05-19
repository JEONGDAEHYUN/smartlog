package com.smartlog.sync.config;

import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그인 실패 핸들러 — 5회 실패 시 30분간 계정 잠금 (DB 영속화)
@Component
@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserService userService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");

        // 이미 잠긴 계정에 로그인 시도 — 카운터 증가 없이 안내만
        if (exception instanceof LockedException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?locked=true");
            return;
        }

        // 비밀번호 불일치 등 일반 실패 → DB 카운터 증가
        if (email != null && !email.isBlank()) {
            userService.recordLoginFailure(email);
        }

        // 현재 실패 횟수 / 잠금 여부 조회 (UI 표시용)
        UserInfo user = (email != null && !email.isBlank())
                ? userService.getEntityByEmail(email) : null;
        int failCount = (user != null && user.getFailCount() != null) ? user.getFailCount() : 0;
        boolean nowLocked = user != null && user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(java.time.LocalDateTime.now());

        if (nowLocked) {
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?locked=true&attempts=" + failCount);
        } else {
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?error=true&attempts=" + failCount);
        }
    }
}
