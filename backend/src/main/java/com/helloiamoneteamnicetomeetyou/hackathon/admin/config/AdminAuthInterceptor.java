package com.helloiamoneteamnicetomeetyou.hackathon.admin.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@code /admin/**} 만 막는다.
 *
 * <p><b>Spring Security 를 넣지 않았다.</b> 필터 체인이 앱 전체를 덮으면서 CSRF 기본값이 지금
 * 인증 없이 도는 {@code /api/**} 의 POST 를 막을 수 있고, CORS 설정과도 얽힌다. 막아야 하는
 * 것이 화면 하나뿐이라 인터셉터로 끝내는 편이 위험이 훨씬 작다고 봤다.
 *
 * <p>로그인하지 않은 요청은 로그인 화면으로 보낸다. 어차피 브라우저로만 들어오는 경로라
 * 401 을 돌려주는 것보다 화면을 주는 쪽이 쓰기 편하다.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (AdminSession.isLoggedIn(request.getSession(false))) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/admin/login");
        return false;
    }
}
