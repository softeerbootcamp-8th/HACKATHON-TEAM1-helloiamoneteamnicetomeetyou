package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.config.AdminProperties;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.config.AdminSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminLoginController {

    private final AdminProperties adminProperties;

    @GetMapping("/admin/login")
    public String loginPage(HttpServletRequest request) {
        if (AdminSession.isLoggedIn(request.getSession(false))) {
            return "redirect:/admin";
        }
        return "admin/login";
    }

    /**
     * 로그인한다.
     *
     * <p><b>세션을 새로 만든다.</b> 로그인 전에 이미 있던 세션을 그대로 쓰면, 남이 심어 둔 세션
     * 아이디로 로그인 상태가 올라타는 일이 생길 수 있다. 여기서 막는 비용이 두 줄이라 넣었다.
     */
    @PostMapping("/admin/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            Model model) {

        if (!adminProperties.matches(username, password)) {
            model.addAttribute("error", "아이디 또는 비밀번호가 맞지 않습니다.");
            return "admin/login";
        }

        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        AdminSession.login(request.getSession(true));

        return "redirect:/admin";
    }

    @GetMapping("/admin/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/admin/login";
    }
}
