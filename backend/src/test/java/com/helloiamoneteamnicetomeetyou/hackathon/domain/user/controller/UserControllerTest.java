package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@DisplayName("사용자 등록 엔드포인트")
class UserControllerTest {

    private static final String BODY = """
            {"userId":"550e8400-e29b-41d4-a716-446655440000"}""";

    private final MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    UserControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("새로 등록하면 201 을 내려준다")
    void 새로_등록하면_201_이다() throws Exception {
        given(userService.register(any(UUID.class))).willReturn(true);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("등록했습니다."))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @DisplayName("이미 등록된 UUID 면 200 을 내려준다")
    void 이미_등록됐으면_200_이다() throws Exception {
        given(userService.register(any(UUID.class))).willReturn(false);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("UUID 형식이 아니면 400 과 팀 에러 형식을 내려준다")
    void UUID_형식이_아니면_400_이다() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000));
    }
}
