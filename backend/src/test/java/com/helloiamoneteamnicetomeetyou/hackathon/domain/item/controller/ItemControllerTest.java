package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.service.ItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemController.class)
@DisplayName("부스 카드 목록 엔드포인트")
class ItemControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Autowired
    ItemControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("카드 목록을 팀 응답 형식으로 내려준다")
    void 카드_목록을_내려준다() throws Exception {
        given(itemService.findByBooth(anyLong()))
                .willReturn(List.of(new ItemResponseDto(7L, "아이오닉 5 N", null, null)));

        mockMvc.perform(get("/api/booths/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].name").value("아이오닉 5 N"))
                // description 과 imageUrl 은 null 이면 응답에서 통째로 빠진다. jsonPath 의
                // doesNotExist() 는 값이 null 이어도 통과해 버리므로, 직렬화된 본문에 키 자체가
                // 없는지를 본다.
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).doesNotContain("description").doesNotContain("imageUrl");
                });
    }

    @Test
    @DisplayName("없는 부스면 404 와 3000 을 내려준다")
    void 없는_부스면_404_다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.BOOTH_NOT_FOUND))
                .given(itemService).findByBooth(anyLong());

        mockMvc.perform(get("/api/booths/999/items"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(3000));
    }

    @Test
    @DisplayName("boothId 가 숫자가 아니면 400 을 내려준다")
    void boothId_가_숫자가_아니면_400_이다() throws Exception {
        mockMvc.perform(get("/api/booths/abc/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1003));
    }
}
