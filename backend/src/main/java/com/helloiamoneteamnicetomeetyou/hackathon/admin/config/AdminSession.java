package com.helloiamoneteamnicetomeetyou.hackathon.admin.config;

import jakarta.servlet.http.HttpSession;

/**
 * 어드민 로그인 여부를 세션에 남기는 자리다.
 *
 * <p>세션 속성 이름이 인터셉터와 로그인 컨트롤러 두 곳에서 쓰이는데, 문자열을 양쪽에 각각
 * 적어 두면 한쪽만 고쳤을 때 아무도 로그인하지 못하는 상태가 조용히 만들어진다.
 */
public final class AdminSession {

    private static final String ATTRIBUTE = "ADMIN_LOGGED_IN";

    private AdminSession() {
    }

    public static void login(HttpSession session) {
        session.setAttribute(ATTRIBUTE, Boolean.TRUE);
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(ATTRIBUTE));
    }
}
