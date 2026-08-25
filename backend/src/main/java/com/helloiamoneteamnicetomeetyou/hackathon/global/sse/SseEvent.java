package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import java.util.UUID;

/**
 * 스프링 이벤트로 오가는 SSE 전송 요청 한 건이다.
 *
 * <p>도메인 코드는 이 타입을 직접 만들지 않고 {@link SseEventPublisher} 를 쓴다.
 *
 * <p>{@code boothId} 와 {@code userId} 중 채워진 쪽이 전송 대상을 정한다. 둘을 함께 받는 대신
 * 정적 팩터리 두 개로 갈라 둬서, 부르는 쪽에서 대상을 헷갈릴 여지를 없앴다.
 */
public record SseEvent(SseEventType type, Object data, Long boothId, UUID userId) {

    public static SseEvent toBooth(Long boothId, SseEventType type, Object data) {
        return new SseEvent(type, data, boothId, null);
    }

    public static SseEvent toUser(UUID userId, SseEventType type, Object data) {
        return new SseEvent(type, data, null, userId);
    }
}
