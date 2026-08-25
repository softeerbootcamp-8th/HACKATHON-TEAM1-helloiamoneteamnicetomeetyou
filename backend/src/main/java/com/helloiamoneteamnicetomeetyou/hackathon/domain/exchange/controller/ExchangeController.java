package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeActorRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.TimeSlotUpdateRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 교환 약속의 장소와 시간.
 *
 * <p>모든 요청이 {@code userId} 를 실어 보낸다. 로그인이 없어서 이게 신원이고, 서버는 그 사람이
 * 이 교환의 참가자인지만 확인한다. 남의 UUID 를 실어 보내면 그 사람인 척할 수 있다는 한계는
 * 사용자 등록 API 와 같다.
 *
 * <p>변경이 일어나면 참가자 전원에게 {@code EXCHANGE_TIME_UPDATED} 가 나간다. 받은 쪽은 이벤트에
 * 담긴 내용을 쓰지 않고 {@code GET /api/exchanges/{exchangeId}} 로 현재 상태를 다시 읽는다.
 * 끊겼던 동안의 이벤트는 다시 오지 않기 때문에 그렇게 해야 화면이 확실히 맞는다.
 */
@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/{exchangeId}")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> find(@PathVariable Long exchangeId) {
        return ResponseEntity.ok(CommonResponse.ok(exchangeService.find(exchangeId), "약속 정보입니다."));
    }

    /** 내가 고른 칸을 통째로 덮어쓴다. 칸 하나를 켜고 끄는 것이 아니라 항상 전체를 보낸다. */
    @PutMapping("/{exchangeId}/time-slots")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> updateTimeSlots(
            @PathVariable Long exchangeId,
            @RequestBody TimeSlotUpdateRequestDto request) {

        ExchangeResponseDto exchange =
                exchangeService.updateTimeSlots(exchangeId, request.userId(), request.slots());

        return ResponseEntity.ok(CommonResponse.ok(exchange, "시간을 저장했습니다."));
    }

    /** "시간 조율 요청하기". 참가자 전원의 선택을 비우고 격자를 지금 기준으로 다시 잡는다. */
    @PostMapping("/{exchangeId}/time-slots/reset")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> resetTimeSlots(
            @PathVariable Long exchangeId,
            @RequestBody ExchangeActorRequestDto request) {

        ExchangeResponseDto exchange = exchangeService.resetTimeSlots(exchangeId, request.userId());

        return ResponseEntity.ok(CommonResponse.ok(exchange, "시간 조율을 요청했습니다."));
    }

    /** 거래 취소. 상대 화면에도 취소됐다는 것이 실시간으로 전해진다. */
    @PostMapping("/{exchangeId}/cancel")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> cancel(
            @PathVariable Long exchangeId,
            @RequestBody ExchangeActorRequestDto request) {

        ExchangeResponseDto exchange = exchangeService.cancel(exchangeId, request.userId());

        return ResponseEntity.ok(CommonResponse.ok(exchange, "약속을 취소했습니다."));
    }

    /** "약속 확정하기". 겹치는 가장 빠른 칸으로 정한다. 참가자 중 한 명만 누르면 된다. */
    @PostMapping("/{exchangeId}/confirm-time")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> confirmTime(
            @PathVariable Long exchangeId,
            @RequestBody ExchangeActorRequestDto request) {

        ExchangeResponseDto exchange = exchangeService.confirmTime(exchangeId, request.userId());

        return ResponseEntity.ok(CommonResponse.ok(exchange, "약속을 확정했습니다."));
    }
}
