package com.helloiamoneteamnicetomeetyou.hackathon.admin.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.controller.AdminLoginController;
import com.helloiamoneteamnicetomeetyou.hackathon.global.common.PingController;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 어드민 인증이 {@code /admin/**} 만 막는지 본다.
 *
 * <p>DB 가 필요 없는 웹 계층 검사라 슬라이스로 좁혔다. 대상을 두 컨트롤러로 한정한 것은
 * {@code CorsConfigTest} 와 같은 이유인데, 지정하지 않으면 슬라이스가 모든 컨트롤러를 올리면서
 * {@code @Service} 를 못 찾아 이 테스트가 같이 깨진다.
 *
 * <p>계정 값을 여기서 직접 준다. {@code application.yml} 의 기본값이 바뀌어도 이 테스트가
 * 흔들리지 않아야 한다.
 */
@WebMvcTest({AdminLoginController.class, PingController.class})
@TestPropertySource(properties = {"admin.username=tester", "admin.password=secret"})
@DisplayName("어드민 인증")
class AdminAuthInterceptorTest {

    private final MockMvc mockMvc;

    @Autowired
    AdminAuthInterceptorTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("로그인하지 않으면 어드민 화면 대신 로그인 화면으로 보낸다")
    void 비로그인_리다이렉트() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    @DisplayName("로그인 화면 자체는 막지 않는다")
    void 로그인_화면은_열려_있다() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 세션을 만들지 않고 로그인 화면에 머문다")
    void 틀린_비밀번호() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/login")
                        .param("username", "tester")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        org.assertj.core.api.Assertions.assertThat(AdminSession.isLoggedIn(session)).isFalse();
    }

    @Test
    @DisplayName("로그인하면 세션이 생기고 그 세션으로 어드민 화면에 들어간다")
    void 로그인_성공() throws Exception {
        MvcResult login = mockMvc.perform(post("/admin/login")
                        .param("username", "tester")
                        .param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();

        HttpSession session = login.getRequest().getSession(false);
        org.assertj.core.api.Assertions.assertThat(AdminSession.isLoggedIn(session)).isTrue();
    }

    /**
     * 어드민을 막느라 기존 API 까지 막으면 프론트가 통째로 멈춘다. Spring Security 를 쓰지 않은
     * 이유가 이것이라, 실제로 그렇게 되는지 확인한다.
     */
    @Test
    @DisplayName("인증 없이 도는 /api 는 그대로 열려 있다")
    void api_는_영향받지_않는다() throws Exception {
        mockMvc.perform(get("/api/ping")).andExpect(status().isOk());
    }
}
