package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto.BoothHaveItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service.BoothHaveItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BoothHaveItemController.class)
@DisplayName("부스 안 다른 사용자 보유 카드 목록 엔드포인트")
class BoothHaveItemControllerTest {

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final UUID OWNER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final MockMvc mockMvc;

    @MockitoBean
    private BoothHaveItemService boothHaveItemService;

    @Autowired
    BoothHaveItemControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("목록을 팀 응답 형식과 목록 규약대로 내려준다")
    void 목록을_규약대로_내려준다() throws Exception {
        BoothHaveItemResponseDto row = new BoothHaveItemResponseDto(
                7L,
                OWNER_ID,
                null,
                new ItemResponseDto(10L, "IONIQ 5 N", null, null),
                2,
                true,
                true,
                List.of("AVANTE N"),
                List.of("AVANTE N", "PONY Vision 74"));

        given(boothHaveItemService.findByBooth(eq(1L), any(UUID.class), anyInt(), anyInt()))
                .willReturn(PageResponse.of(List.of(row), false));

        mockMvc.perform(get("/api/booths/1/have-items").param("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회했습니다."))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].haveItemId").value(7))
                .andExpect(jsonPath("$.data.content[0].ownerId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].item.name").value("IONIQ 5 N"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(2))
                .andExpect(jsonPath("$.data.content[0].wanted").value(true))
                .andExpect(jsonPath("$.data.content[0].matched").value(true))
                .andExpect(jsonPath("$.data.content[0].givableItemNames[0]").value("AVANTE N"))
                .andExpect(jsonPath("$.data.content[0].ownerWantedItemNames.length()").value(2))
                // 아직 채우지 않는 값은 @JsonInclude(NON_NULL) 로 응답에서 빠진다.
                .andExpect(jsonPath("$.data.content[0].ownerName").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].item.imageUrl").doesNotExist());
    }

    @Test
    @DisplayName("userId 를 빠뜨리면 400 과 어느 파라미터인지 함께 내려준다")
    void userId_가_없으면_400_이다() throws Exception {
        mockMvc.perform(get("/api/booths/1/have-items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.errors[0].field").value("userId"));
    }

    @Test
    @DisplayName("userId 가 UUID 형식이 아니면 400 이다")
    void userId_가_UUID_가_아니면_400_이다() throws Exception {
        mockMvc.perform(get("/api/booths/1/have-items").param("userId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @DisplayName("없는 부스면 404 와 팀 에러 형식을 내려준다")
    void 없는_부스면_404_이다() throws Exception {
        given(boothHaveItemService.findByBooth(eq(99L), any(UUID.class), anyInt(), anyInt()))
                .willThrow(new ApplicationException(ErrorCode.BOOTH_NOT_FOUND));

        mockMvc.perform(get("/api/booths/99/have-items").param("userId", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(3000))
                .andExpect(jsonPath("$.message").value("부스를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("page 와 size 를 안 보내면 기본값 0, 20 으로 부른다")
    void 기본_페이지_값을_쓴다() throws Exception {
        given(boothHaveItemService.findByBooth(eq(1L), any(UUID.class), eq(0), eq(20)))
                .willReturn(PageResponse.of(List.of(), false));

        mockMvc.perform(get("/api/booths/1/have-items").param("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(0));
    }
}
