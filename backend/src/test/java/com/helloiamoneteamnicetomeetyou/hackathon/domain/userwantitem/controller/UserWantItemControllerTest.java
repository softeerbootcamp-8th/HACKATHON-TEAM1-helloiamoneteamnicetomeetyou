package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service.UserWantItemService;
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

@WebMvcTest(UserWantItemController.class)
@DisplayName("희망 카드 등록 엔드포인트")
class UserWantItemControllerTest {

    private static final String BODY = """
            {"userId":"550e8400-e29b-41d4-a716-446655440000","itemId":1,"quantity":2}""";

    private final MockMvc mockMvc;

    @MockitoBean
    private UserWantItemService userWantItemService;

    @Autowired
    UserWantItemControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("새로 등록하면 201 을 내려준다")
    void 새로_등록하면_201_이다() throws Exception {
        given(userWantItemService.register(any(UUID.class), anyLong(), any())).willReturn(true);

        mockMvc.perform(post("/api/want-items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("등록했습니다."));
    }

    @Test
    @DisplayName("이미 등록됐으면 200 을 내려준다")
    void 이미_등록됐으면_200_이다() throws Exception {
        given(userWantItemService.register(any(UUID.class), anyLong(), any())).willReturn(false);

        mockMvc.perform(post("/api/want-items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("등록되지 않은 사용자면 404 를 내려준다")
    void 등록되지_않은_사용자면_404_다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.USER_NOT_FOUND))
                .given(userWantItemService).register(any(UUID.class), anyLong(), any());

        mockMvc.perform(post("/api/want-items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
