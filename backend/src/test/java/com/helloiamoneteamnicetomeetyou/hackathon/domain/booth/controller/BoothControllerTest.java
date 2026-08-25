package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.BoothResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.service.BoothService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BoothController.class)
@DisplayName("부스 목록 엔드포인트")
class BoothControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private BoothService boothService;

    @Autowired
    BoothControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("부스 목록을 팀 응답 형식으로 내려준다")
    void 부스_목록을_내려준다() throws Exception {
        given(boothService.findAll())
                .willReturn(List.of(new BoothResponseDto(1L, "현대차 팝업", null)));

        mockMvc.perform(get("/api/booths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("현대차 팝업"))
                // description 이 null 이면 키째로 빠진다. doesNotExist() 는 null 에도 통과해
                // 버려서 본문을 직접 본다.
                .andExpect(result ->
                        assertThat(result.getResponse().getContentAsString())
                                .doesNotContain("description"));
    }

    @Test
    @DisplayName("부스가 없으면 빈 목록이다")
    void 부스가_없으면_빈_목록이다() throws Exception {
        given(boothService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/booths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
