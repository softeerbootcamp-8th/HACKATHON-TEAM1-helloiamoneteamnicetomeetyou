package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeAnswerResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeSendResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.ReceivedPokeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.service.PokeService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PokeController.class)
@DisplayName("찔러보기 엔드포인트")
class PokeControllerTest {

    private static final String SENDER = "11111111-1111-1111-1111-111111111111";
    private static final String RECEIVER = "22222222-2222-2222-2222-222222222222";

    private static final String SEND_BODY = """
            {"userId":"%s","targetUserId":"%s","requestedItemId":10}"""
            .formatted(SENDER, RECEIVER);

    private final MockMvc mockMvc;

    @MockitoBean
    private PokeService pokeService;

    @Autowired
    PokeControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("보내면 201 과 pokeId 를 내려준다")
    void 보내면_201_이다() throws Exception {
        given(pokeService.send(any(UUID.class), any(UUID.class), eq(10L)))
                .willReturn(new PokeSendResponseDto(7L));

        mockMvc.perform(post("/api/pokes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SEND_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("찔러봤습니다."))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.pokeId").value(7));
    }

    @Test
    @DisplayName("같은 상대에게 대기 중이면 409 와 팀 에러 형식을 내려준다")
    void 중복이면_409_이다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.POKE_DUPLICATE_PENDING))
                .given(pokeService).send(any(UUID.class), any(UUID.class), eq(10L));

        mockMvc.perform(post("/api/pokes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SEND_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(4012))
                .andExpect(jsonPath("$.message").value("이미 답변을 기다리는 찔러보기가 있습니다."));
    }

    @Test
    @DisplayName("받은 목록은 상대가 원하는 카드와 내놓은 묶음을 함께 내려준다")
    void 받은_목록을_내려준다() throws Exception {
        ReceivedPokeResponseDto row = new ReceivedPokeResponseDto(
                7L,
                UUID.fromString(SENDER),
                null,
                new ItemResponseDto(10L, "AVANTE N", null, null, "AN"),
                List.of(new ItemResponseDto(20L, "i20 N", null, null, "I20N"),
                        new ItemResponseDto(30L, "i30 N", null, null, "I30N")),
                LocalDateTime.of(2026, 8, 25, 12, 0));

        given(pokeService.findReceived(any(UUID.class), anyInt(), anyInt()))
                .willReturn(PageResponse.of(List.of(row), false));

        mockMvc.perform(get("/api/pokes/received").param("userId", RECEIVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].pokeId").value(7))
                .andExpect(jsonPath("$.data.content[0].requestedItem.name").value("AVANTE N"))
                .andExpect(jsonPath("$.data.content[0].offeredItems.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].fromUserName").doesNotExist());
    }

    @Test
    @DisplayName("userId 를 빠뜨리면 400 과 어느 파라미터인지 함께 내려준다")
    void userId_가_없으면_400_이다() throws Exception {
        mockMvc.perform(get("/api/pokes/received"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.errors[0].field").value("userId"));
    }

    @Test
    @DisplayName("수락하면 오갈 카드와 교환 번호를 내려준다")
    void 수락하면_교환_번호를_내려준다() throws Exception {
        given(pokeService.answer(eq(7L), any(UUID.class), eq(PokeStatus.ACCEPTED), eq(20L)))
                .willReturn(new PokeAnswerResponseDto(7L, PokeStatus.ACCEPTED, 3L, 10L, 20L));

        mockMvc.perform(patch("/api/pokes/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","status":"ACCEPTED","chosenItemId":20}"""
                                .formatted(RECEIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.exchangeId").value(3))
                .andExpect(jsonPath("$.data.giveItemId").value(10))
                .andExpect(jsonPath("$.data.receiveItemId").value(20));
    }

    @Test
    @DisplayName("거절하면 교환 정보가 응답에서 빠진다")
    void 거절하면_교환_정보가_없다() throws Exception {
        given(pokeService.answer(eq(7L), any(UUID.class), eq(PokeStatus.REJECTED), any()))
                .willReturn(new PokeAnswerResponseDto(7L, PokeStatus.REJECTED, null, null, null));

        mockMvc.perform(patch("/api/pokes/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","status":"REJECTED"}""".formatted(RECEIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.exchangeId").doesNotExist())
                .andExpect(jsonPath("$.data.receiveItemId").doesNotExist());
    }

    @Test
    @DisplayName("남의 찔러보기에 응답하면 403 이다")
    void 받은_사람이_아니면_403_이다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.POKE_NOT_RECEIVER))
                .given(pokeService)
                .answer(eq(7L), any(UUID.class), eq(PokeStatus.REJECTED), any());

        mockMvc.perform(patch("/api/pokes/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","status":"REJECTED"}""".formatted(SENDER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(4014));
    }

    @Test
    @DisplayName("status 가 알 수 없는 값이면 400 이다")
    void 알_수_없는_status_면_400_이다() throws Exception {
        mockMvc.perform(patch("/api/pokes/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","status":"WHATEVER"}""".formatted(RECEIVER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000));
    }
}
