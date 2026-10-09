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
// 호출 시점: Spring Security가 비밀번호 검증 등에서 인증을 실패시키면 이 핸들러를 "자동으로" 실행
//          (SecurityConfig 의 .failureHandler(loginFailureHandler) 로 연결되어 있어서 호출됨)

// @Component : 스프링 빈으로 등록 → SecurityConfig 가 주입받아 failureHandler 로 사용
@Component
// @RequiredArgsConstructor : final 필드(userService)를 받는 생성자를 Lombok이 자동 생성 (생성자 주입)
@RequiredArgsConstructor
// extends SimpleUrlAuthenticationFailureHandler
//  - 스프링 시큐리티가 제공하는 "로그인 실패 후 처리" 기본 핸들러를 상속
//  - 기본 리다이렉트 기능(getRedirectStrategy)을 그대로 쓰고, 우리는 "실패 카운트/잠금" 로직을 추가
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler { // 비밀번호 틀림 등 인증 실패하면 LoginFailuerHandler 실행

    // 실패 카운트 증가/잠금 같은 회원 비즈니스 로직을 호출하기 위해 UserService 주입
    private final UserService userService;

    // 로그인 실패 시 스프링 시큐리티가 자동으로 호출하는 메서드 (오버라이드)
    //  - request   : 들어온 요청 (여기서 사용자가 입력한 username 을 꺼냄)
    //  - response  : 응답 (실패 시 어느 화면으로 보낼지 리다이렉트에 사용)
    //  - exception : 실패 원인(예: 비번 불일치 / 계정 잠김 LockedException 등)
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // username 은 Spring Security가 로그인 ID를 부르는 이름 (폼의 name="username")
        // 따라서 username은 이메일이다. (사용자가 입력한 로그인 ID = 이메일)
        String email = request.getParameter("username");

        // [분기 ①] 이미 잠긴 계정에 로그인 시도 — 카운터 증가 없이 안내만
        //  - exception 이 LockedException 이면 "이미 잠긴 상태"라는 뜻
        //  - 잠긴 동안엔 실패 카운트를 더 올리는 게 의미 없으므로 증가시키지 않고 잠금 안내 페이지로만 보냄
        //  - return 으로 메서드 즉시 종료 (아래 로직 실행 안 함)
        if (exception instanceof LockedException) {
            // 관리자 잠금(failCount < 5)과 5회 실패 잠금을 구분하여 다른 안내 표시
            UserInfo lockedUser = (email != null && !email.isBlank())
                    ? userService.getEntityByEmail(email) : null;
            boolean adminLocked = lockedUser != null
                    && (lockedUser.getFailCount() == null || lockedUser.getFailCount() < 5);
            String redirectUrl = adminLocked ? "/login?adminLocked=true" : "/login?locked=true";
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        // [분기 ②] 비밀번호 불일치 등 "일반 실패" → DB 실패 카운터 증가
        //  - email 이 null/빈값이 아니면(정상 입력) recordLoginFailure 호출
        //  - recordLoginFailure 내부: failCount +1, 5회 도달 시 lockFor(30) 으로 30분 잠금
        if (email != null && !email.isBlank()) {
            userService.recordLoginFailure(email);
        }

        // [현재 상태 조회] 방금 실패를 반영한 뒤의 실패 횟수 / 잠금 여부를 다시 읽음 (화면 표시용)
        //  - getEntityByEmail: 이메일로 회원 엔티티 조회 (없으면 null)
        UserInfo user = (email != null && !email.isBlank())
                ? userService.getEntityByEmail(email) : null;

        // failCount: 현재 실패 횟수 (user 없거나 null이면 0으로 안전 처리)
        int failCount = (user != null && user.getFailCount() != null) ? user.getFailCount() : 0;

        // nowLocked: 이번 실패로 "지금 잠금 상태가 되었는가" 판단
        //  - lockedUntil(잠금 해제 시각)이 현재 시각보다 미래(isAfter)면 → 잠금 중
        boolean nowLocked = user != null && user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(java.time.LocalDateTime.now());

        // [분기 ③] 결과에 따라 다른 안내 페이지로 리다이렉트 (쿼리스트링으로 상태 전달)
        if (nowLocked) {
            // 이번 실패로 잠긴 경우 → 잠금 안내 + 현재 실패 횟수
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?locked=true&attempts=" + failCount);
        } else {
            // 아직 안 잠긴 일반 실패 → 에러 안내 + 현재 실패 횟수 (예: "시도 3/5")
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?error=true&attempts=" + failCount);
        }
    }
}
