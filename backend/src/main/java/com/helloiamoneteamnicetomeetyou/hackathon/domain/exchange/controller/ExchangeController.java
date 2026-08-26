package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantActionRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참가자 본인이 매칭 결과에 반응하는 자리다. 대리 조작은 {@code /admin/exchanges} 가 맡는다. */
@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping("/{exchangeId}/accept")
    public CommonResponse<Void> accept(
            @PathVariable Long exchangeId, @RequestBody ExchangeParticipantActionRequestDto request) {
        exchangeService.accept(exchangeId, request.userId());
        return CommonResponse.ok("수락했습니다.");
    }

    @PostMapping("/{exchangeId}/reject")
    public CommonResponse<Void> reject(
            @PathVariable Long exchangeId, @RequestBody ExchangeParticipantActionRequestDto request) {
        exchangeService.reject(exchangeId, request.userId());
        return CommonResponse.ok("거절했습니다.");
    }
}
