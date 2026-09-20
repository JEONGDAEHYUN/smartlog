package com.smartlog.sync.config;

import com.smartlog.sync.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그인 성공 핸들러 — 로그인에 성공했을 때 실행되는 클래스
// 역할: DB의 실패 카운터(failCount) / 잠금 상태(lockedUntil)를 초기화하고 대시보드로 이동
// 호출 시점: Spring Security가 비밀번호 검증까지 성공시키면, 이 핸들러를 "자동으로" 실행함
//          (SecurityConfig 의 .successHandler(loginSuccessHandler) 로 연결되어 있어서 호출됨)

// @Component : 이 클래스를 스프링 빈으로 등록 → SecurityConfig 가 주입받아 successHandler 로 사용
@Component
// extends SimpleUrlAuthenticationSuccessHandler
//  - 스프링 시큐리티가 제공하는 "로그인 성공 후 URL 이동" 기본 핸들러를 상속
//  - 기본 이동/리다이렉트 기능은 그대로 쓰고, 우리는 "실패 카운트 리셋" 로직만 추가로 끼워넣음
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // 회원 비즈니스 로직(실패 카운트 리셋 등)을 호출하기 위해 UserService 를 주입받음
    private final UserService userService;

    // 생성자 주입 — 객체가 만들어질 때 UserService 를 받고, 성공 후 이동 설정도 여기서 지정
    public LoginSuccessHandler(UserService userService) {
        this.userService = userService;
        // 로그인 성공 시 이동할 기본 경로를 /dashboard 로 지정
        setDefaultTargetUrl("/dashboard");
        // true 로 두면 "원래 가려던 페이지" 와 상관없이 "항상 /dashboard 로" 강제 이동
        //  - false 면: 로그인 전에 접근하려던 보호 페이지가 있으면 그쪽으로 보냄
        //  - true  면: 로그인 성공 후 무조건 대시보드로 통일 (우리 정책)
        setAlwaysUseDefaultTargetUrl(true);
    }

    // 로그인 성공 시 스프링 시큐리티가 자동으로 호출하는 메서드 (오버라이드)
    //  - HttpServletRequest  request        : 들어온 요청 정보
    //  - HttpServletResponse response        : 내보낼 응답 정보(리다이렉트 등에 사용)
    //  - Authentication      authentication  : 인증에 성공한 사용자 정보(이름=로그인 ID=이메일 보유)
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // authentication.getName() = 로그인에 성공한 사용자의 username(우리 프로젝트에선 이메일)
        // resetLoginFailures(email) → DB 의 failCount 를 0, lockedUntil 을 null 로 초기화
        //  (그래야 다음 로그인이 깨끗한 상태에서 시작됨 — 이전 실패 기록/잠금 흔적 제거)
        userService.resetLoginFailures(authentication.getName());

        // 부모(SimpleUrlAuthenticationSuccessHandler)의 기본 동작 실행
        //  → 위에서 지정한 /dashboard 로 실제 리다이렉트 처리
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
