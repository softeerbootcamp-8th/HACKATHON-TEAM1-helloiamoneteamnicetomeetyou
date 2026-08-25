package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.ZoneResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
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

@WebMvcTest(ExchangeController.class)
@DisplayName("교환 약속 엔드포인트")
class ExchangeControllerTest {

    private static final UUID ME = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String SLOTS_BODY = """
            {"userId":"11111111-1111-4111-8111-111111111111","slots":[0,2]}""";
    private static final String ACTOR_BODY = """
            {"userId":"11111111-1111-4111-8111-111111111111"}""";

    private final MockMvc mockMvc;

    @MockitoBean
    private ExchangeService exchangeService;

    @Autowired
    ExchangeControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static ExchangeResponseDto sample(Integer overlapSlot, LocalDateTime confirmedTime) {
        return new ExchangeResponseDto(
                1L,
                1L,
                ExchangeType.ONE_TO_ONE,
                ExchangeStatus.PENDING,
                new ZoneResponseDto(1L, "중앙 포토존 앞", "행사 중앙 포토존", 52, 44),
                LocalDateTime.of(2026, 8, 25, 14, 15),
                8,
                15,
                3,
                28,
                List.of(new ExchangeParticipantResponseDto(ME, "레몬 28", List.of(0, 2), true, false)),
                overlapSlot,
                true,
                confirmedTime);
    }

    @Test
    @DisplayName("약속을 읽으면 격자 기준 시각을 함께 내려준다")
    void 약속을_읽으면_격자_기준_시각도_준다() throws Exception {
        given(exchangeService.find(1L)).willReturn(sample(0, null));

        mockMvc.perform(get("/api/exchanges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slotBaseTime").value("2026-08-25T14:15:00"))
                .andExpect(jsonPath("$.data.slotCount").value(8))
                .andExpect(jsonPath("$.data.slotMinutes").value(15))
                .andExpect(jsonPath("$.data.zone.name").value("중앙 포토존 앞"))
                .andExpect(jsonPath("$.data.participants[0].slots[1]").value(2));
    }

    @Test
    @DisplayName("고른 시간을 저장하면 갱신된 약속을 돌려준다")
    void 고른_시간을_저장한다() throws Exception {
        given(exchangeService.updateTimeSlots(anyLong(), any(UUID.class), any())).willReturn(sample(0, null));

        mockMvc.perform(put("/api/exchanges/1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON).content(SLOTS_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overlapSlot").value(0));
    }

    @Test
    @DisplayName("약속을 확정하면 만나는 시각이 채워진다")
    void 약속을_확정한다() throws Exception {
        given(exchangeService.confirmTime(anyLong(), any(UUID.class)))
                .willReturn(sample(0, LocalDateTime.of(2026, 8, 25, 14, 15)));

        mockMvc.perform(post("/api/exchanges/1/confirm-time")
                        .contentType(MediaType.APPLICATION_JSON).content(ACTOR_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedTime").value("2026-08-25T14:15:00"));
    }

    @Test
    @DisplayName("참가자가 아니면 403 과 팀 에러 형식을 내려준다")
    void 참가자가_아니면_403_이다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.NOT_EXCHANGE_PARTICIPANT))
                .given(exchangeService).updateTimeSlots(anyLong(), any(UUID.class), any());

        mockMvc.perform(put("/api/exchanges/1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON).content(SLOTS_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    @DisplayName("겹치는 칸이 없으면 확정 요청이 409 다")
    void 겹치는_칸이_없으면_409_이다() throws Exception {
        willThrow(new ApplicationException(ErrorCode.NO_OVERLAPPING_TIME))
                .given(exchangeService).confirmTime(anyLong(), any(UUID.class));

        mockMvc.perform(post("/api/exchanges/1/confirm-time")
                        .contentType(MediaType.APPLICATION_JSON).content(ACTOR_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(4003));
    }
}
