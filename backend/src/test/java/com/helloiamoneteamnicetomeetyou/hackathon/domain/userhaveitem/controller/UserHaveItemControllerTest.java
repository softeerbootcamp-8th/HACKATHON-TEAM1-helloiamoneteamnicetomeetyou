package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service.UserHaveItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserHaveItemController.class)
@DisplayName("내놓을 카드 등록 엔드포인트")
class UserHaveItemControllerTest {

    private static final String BODY = """
            {"userId":"550e8400-e29b-41d4-a716-446655440000","itemId":1,"quantity":2}""";

    private final MockMvc mockMvc;

    @MockitoBean
    private UserHaveItemService userHaveItemService;

    @Autowired
    UserHaveItemControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("새로 등록하면 201 을 내려준다")
    void 새로_등록하면_201_이다() throws Exception {
        given(userHaveItemService.register(any(UUID.class), anyLong(), any())).willReturn(true);

        mockMvc.perform(post("/api/have-items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("등록했습니다."));
    }

    @Test
    @DisplayName("이미 등록됐으면 200 을 내려준다")
    void 이미_등록됐으면_200_이다() throws Exception {
        given(userHaveItemService.register(any(UUID.class), anyLong(), any())).willReturn(false);

        mockMvc.perform(post("/api/have-items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("등록되지 않은 사용자면 404 와 팀 에러 형식을 내려준다")
    void 등록되지_않은_사용자면_404_다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.USER_NOT_FOUND))
                .given(userHaveItemService).register(any(UUID.class), anyLong(), any());

        mockMvc.perform(post("/api/have-items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    @DisplayName("존재하지 않는 카드면 404 와 5000 을 내려준다")
    void 존재하지_않는_카드면_404_다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.ITEM_NOT_FOUND))
                .given(userHaveItemService).register(any(UUID.class), anyLong(), any());

        mockMvc.perform(post("/api/have-items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(5000));
    }

    @Test
    @DisplayName("개수가 숫자가 아니면 400 을 내려준다")
    void 개수가_숫자가_아니면_400_이다() throws Exception {
        mockMvc.perform(post("/api/have-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"550e8400-e29b-41d4-a716-446655440000","itemId":1,"quantity":"two"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000));
    }
}
