package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 다른 도메인이 실시간 알림을 보낼 때 쓰는 유일한 문이다.
 *
 * <p>매칭이든 약속 시간 변경이든, 서비스는 이것 하나만 주입받아 부르면 된다. 연결을 누가 들고
 * 있는지, 어떤 스레드에서 나가는지는 이 아래에서 처리한다.
 *
 * <pre>
 * sseEventPublisher.toBooth(boothId, SseEventType.USER_JOINED, dto);
 * sseEventPublisher.toUser(userId, SseEventType.MATCH_SUGGESTED, dto);
 * </pre>
 *
 * <p><b>바로 보내지 않고 스프링 이벤트로 한 번 감싼다.</b> {@link SseEventDispatcher} 가 트랜잭션
 * 커밋 이후에 받게 하기 위해서다. 서비스 안에서 곧바로 전송하면 저장이 롤백된 매칭을 화면이 먼저
 * 받아 버리는 경우가 생긴다.
 */
@Component
@RequiredArgsConstructor
public class SseEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 부스에 접속한 모두에게 보낸다.
     *
     * <p>구역({@code Zone})이 아니라 부스가 단위인 것에 주의한다. 구역은 지도에서 고르는 만나는
     * 자리라 사람이 옮겨 다니고, 구역 이야기를 담은 이벤트도 결국 같은 부스 사람들이 봐야 한다.
     * 특정 구역에 대한 이벤트면 그 구역 정보를 {@code data} 에 담아 보내면 된다.
     */
    public void toBooth(Long boothId, SseEventType type, Object data) {
        applicationEventPublisher.publishEvent(SseEvent.toBooth(boothId, type, data));
    }

    /** 특정 사용자에게만 보낸다. 그 사용자가 여러 탭을 열어 뒀으면 전부 받는다. */
    public void toUser(UUID userId, SseEventType type, Object data) {
        applicationEventPublisher.publishEvent(SseEvent.toUser(userId, type, data));
    }
}
