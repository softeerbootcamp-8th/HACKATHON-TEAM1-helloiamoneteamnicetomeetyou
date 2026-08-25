package com.helloiamoneteamnicetomeetyou.hackathon.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.global.common.PingController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

// JPA 가 들어온 뒤로 @SpringBootTest 는 실제 MySQL 을 찾는다. CORS 는 웹 계층 설정이라
// 웹 슬라이스만 띄운다. @WebMvcTest 는 WebMvcConfigurer 를 그대로 집어 온다.
//
// 대상을 PingController 하나로 좁힌 것은, 인자 없이 쓰면 모든 Controller 를 슬라이스에 올려서
// 그 Controller 가 의존하는 빈까지 전부 있어야 하기 때문이다. 여기서 확인하는 것은 /api/ping
// 하나에 붙는 CORS 헤더라, 컨트롤러가 새로 생길 때마다 이 테스트가 깨질 이유가 없다.
@WebMvcTest(PingController.class)
@DisplayName("CORS 설정")
class CorsConfigTest {

    private final MockMvc mockMvc;

    @Autowired
    CorsConfigTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("Vercel 프리뷰 도메인에서 온 프리플라이트를 허용한다")
    void vercel_프리뷰_프리플라이트_허용() throws Exception {
        mockMvc.perform(options("/api/ping")
                        .header(HttpHeaders.ORIGIN, "https://team1-git-feat-abc123.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://team1-git-feat-abc123.vercel.app"));
    }

    @Test
    @DisplayName("Vite 개발 서버에서 온 요청에 허용 헤더를 붙인다")
    void 로컬_개발서버_요청_허용() throws Exception {
        mockMvc.perform(get("/api/ping").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    @DisplayName("허용 목록에 없는 오리진의 프리플라이트는 거절한다")
    void 허용하지_않은_오리진_거절() throws Exception {
        mockMvc.perform(options("/api/ping")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
