package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.demo;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 교환을 만드는 임시 엔드포인트다. 매칭 알고리즘(이슈 #20)이 붙으면 지운다.
 *
 * <p>지금은 화면이 매칭을 목업으로 돌리기 때문에, 상대가 정해진 뒤 교환 행을 만들어 줄 곳이 없다.
 * 시간과 장소를 붙일 대상이 있어야 해서 이 문을 열어 뒀다.
 *
 * <p><b>여기에 매칭 규칙을 넣지 않는다.</b> 누구와 교환할지는 #20 이 정하고, 정해진 다음에
 * {@code ExchangeService.create(...)} 를 부르면 된다. 그 메서드는 정식 코드라 그대로 남는다.
 */
@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
public class ExchangeDemoController {

    private final ExchangeService exchangeService;

    @PostMapping
    public ResponseEntity<CommonResponse<ExchangeResponseDto>> create(
            @RequestBody ExchangeCreateRequestDto request) {

        ExchangeResponseDto exchange = exchangeService.create(
                request.boothId(), request.type(), request.participantUserIds());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(exchange, "교환을 만들었습니다."));
    }
}
