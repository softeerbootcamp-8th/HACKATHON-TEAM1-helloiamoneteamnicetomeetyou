package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeActorRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantActionRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.TimeSlotUpdateRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 교환 약속의 장소와 시간.
 *
 * <p><b>만날 자리를 바꾸는 길은 여기 없다.</b> 자리는 팝업 운영자가 미리 정해 둔 한 곳이고, 사용자
 * 화면은 그 자리를 확인만 한다. 옮겨야 하면 어드민 콘솔에서 옮긴다.
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

    /**
     * 내가 지금 잡고 있는 약속. 없으면 204 다.
     *
     * <p>앱을 열 때와 실시간 연결이 붙을 때 부른다. 화면 상태가 메모리에만 있어서 새로고침 한 번에
     * 사라지는데, 이게 없으면 진행 중인 약속으로 돌아올 방법이 없다.
     *
     * <p>{@code /{exchangeId}} 보다 먼저 선언한다. 뒤에 두면 "active" 를 경로 변수로 읽으려다
     * 타입이 안 맞아 400 이 난다.
     */
    @GetMapping("/active")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> findActive(@RequestParam UUID userId) {
        ExchangeResponseDto exchange = exchangeService.findActiveOf(userId);

        if (exchange == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(CommonResponse.ok(exchange, "진행 중인 약속입니다."));
    }

    /**
     * 매칭 결과를 받아들이고 장소를 잡으러 들어간다.
     *
     * <p>상대의 수락을 기다리지 않는다. 각자 장소와 시간 화면으로 들어가 맞춰 보는 흐름이라
     * 둘 다 눌러야 진행되는 조건을 걸 이유가 없다.
     */
    @PostMapping("/{exchangeId}/accept")
    public CommonResponse<Void> accept(
            @PathVariable Long exchangeId, @RequestBody ExchangeParticipantActionRequestDto request) {
        exchangeService.accept(exchangeId, request.userId());
        return CommonResponse.ok("수락했습니다.");
    }

    /** 매칭 결과를 거절한다. 나머지 참가자는 다시 상대를 찾는다. */
    @PostMapping("/{exchangeId}/reject")
    public CommonResponse<Void> reject(
            @PathVariable Long exchangeId, @RequestBody ExchangeParticipantActionRequestDto request) {
        exchangeService.reject(exchangeId, request.userId());
        return CommonResponse.ok("거절했습니다.");
    }

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

    /**
     * "시간 조율 요청하기". <b>상대에게 알림만 보낸다.</b> 고른 칸은 양쪽 모두 그대로 남는다.
     *
     * <p>경로에 남아 있는 {@code reset} 은 전원의 선택을 비우던 예전 동작에서 온 이름이다.
     * 프론트가 이미 이 경로를 부르고 있어서 이름만 바꾸지 않았다. 팀에서 합의되면
     * {@code /time-request} 로 옮긴다.
     */
    @PostMapping("/{exchangeId}/time-slots/reset")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> requestTimeCoordination(
            @PathVariable Long exchangeId,
            @RequestBody ExchangeActorRequestDto request) {

        ExchangeResponseDto exchange =
                exchangeService.requestTimeCoordination(exchangeId, request.userId());

        return ResponseEntity.ok(CommonResponse.ok(exchange, "시간 조율을 요청했습니다."));
    }

    /** "도착했어요". 상대 화면의 배지가 이동중에서 도착으로 바뀐다. */
    @PostMapping("/{exchangeId}/arrive")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> arrive(
            @PathVariable Long exchangeId,
            @RequestBody ExchangeActorRequestDto request) {

        ExchangeResponseDto exchange = exchangeService.arrive(exchangeId, request.userId());

        return ResponseEntity.ok(CommonResponse.ok(exchange, "도착을 알렸습니다."));
    }

    /**
     * "만났어요". 교환이 끝났다는 것을 남긴다.
     *
     * <p>한 명이 이걸 누르고 다른 한 명이 취소를 누를 수 있어서, 먼저 도착한 한 번만 반영된다.
     * 늦은 쪽은 409 를 받고 화면을 현재 상태로 맞춘다.
     */
    @PostMapping("/{exchangeId}/complete")
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> complete(
            @PathVariable Long exchangeId,
            @RequestBody ExchangeActorRequestDto request) {

        ExchangeResponseDto exchange = exchangeService.complete(exchangeId, request.userId());

        return ResponseEntity.ok(CommonResponse.ok(exchange, "교환을 마쳤습니다."));
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
