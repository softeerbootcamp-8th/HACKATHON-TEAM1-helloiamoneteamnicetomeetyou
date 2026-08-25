package com.helloiamoneteamnicetomeetyou.hackathon.global.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("연동 확인 엔드포인트")
class PingControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    PingControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("팀 공통 응답 형식으로 pong 과 서버 시각을 내려준다")
    void ping_응답_형식() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("pong"))
                .andExpect(jsonPath("$.data.serverTime").isString())
                .andExpect(jsonPath("$.message").value("연결되었습니다."));
    }

    @Test
    @DisplayName("성공 응답에는 code 와 errors 를 담지 않는다")
    void 성공_응답에는_에러_필드가_없다() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }
}
