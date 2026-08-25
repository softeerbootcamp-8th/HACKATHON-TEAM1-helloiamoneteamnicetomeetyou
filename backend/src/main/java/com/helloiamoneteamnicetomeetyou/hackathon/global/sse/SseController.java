package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 부스 실시간 알림 구독 엔드포인트다.
 *
 * <p>응답이 끝나지 않는 스트림이라 {@code CommonResponse} 로 감싸지 않는다. 팀 응답 형식은 한 번에
 * 끝나는 응답을 위한 것이고, 여기서 오가는 각 이벤트의 형식은 그 이벤트를 보내는 도메인이 정한다.
 */
@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class SseController {

    private final SseConnectionManager connectionManager;

    /**
     * 부스 하나를 구독한다. 사용자가 지도에서 구역을 옮겨 다녀도 이 연결은 그대로 유지된다.
     *
     * <p>{@code userId} 를 쿼리 파라미터로 받는다. GET 은 본문을 실을 수 없어서 정한 팀 규칙이기도
     * 하고, 브라우저의 {@code EventSource} 가 헤더를 붙일 방법을 아예 주지 않아서 다른 선택지가
     * 없기도 하다.
     */
    @GetMapping(value = "/{boothId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable Long boothId,
            @RequestParam UUID userId,
            HttpServletResponse response) {

        // 앞단 프록시가 응답을 버퍼에 모았다가 내보내면, 끝나지 않는 스트림인 SSE 는 버퍼가 찰
        // 일이 없어 브라우저에 한 바이트도 가지 않는다. 지금 쓰는 Caddy 는 text/event-stream 을
        // 알아서 흘려보내지만, 이 헤더를 보는 프록시(nginx 등)로 바뀌어도 그대로 돌게 붙여 둔다.
        response.setHeader("X-Accel-Buffering", "no");

        return connectionManager.subscribe(boothId, userId);
    }
}
