package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 열려 있는 SSE 연결 하나다. 누가 어느 부스에 붙어 있는지와 실제로 바이트를 내보내는
 * {@link SseEmitter} 를 함께 들고 있다.
 *
 * <p>{@code equals} 를 재정의하지 않는다. 같은 사용자가 탭을 두 개 열면 연결도 두 개고, 그 둘은
 * 각각 따로 정리돼야 하기 때문에 객체 동일성이 그대로 식별자가 되는 편이 맞다.
 */
@Slf4j
@Getter
public class SseConnection {

    private final Long boothId;
    private final UUID userId;
    private final SseEmitter emitter;

    /**
     * 이 연결로 나가는 전송을 한 번에 하나만 하도록 막는다.
     *
     * <p>{@link SseEmitter#send} 는 스레드 안전하지 않아서, 같은 연결에 두 이벤트가 동시에
     * 나가면 바이트가 섞여 클라이언트가 깨진 프레임을 받는다. 브로드캐스트를 연결마다 별도
     * 스레드로 흘리기 때문에 실제로 겹칠 수 있다.
     *
     * <p><b>{@code synchronized} 대신 {@link ReentrantLock} 을 쓴다.</b> Java 21 의 가상 스레드는
     * 모니터를 잡은 채 블로킹하면 캐리어 스레드에 고정(pinning)되는데, 여기서 블로킹하는 것이
     * 하필 느린 클라이언트로의 소켓 write 다. 그러면 가상 스레드를 쓰는 이유가 통째로 사라진다.
     * {@code ReentrantLock} 은 대기할 때 캐리어를 반납한다.
     */
    private final ReentrantLock sendLock = new ReentrantLock();

    public SseConnection(Long boothId, UUID userId, SseEmitter emitter) {
        this.boothId = boothId;
        this.userId = userId;
        this.emitter = emitter;
    }

    void send(SseEventType type, Object data) throws IOException {
        sendLock.lock();
        try {
            emitter.send(SseEmitter.event().name(type.name()).data(data));
        } finally {
            sendLock.unlock();
        }
    }

    /**
     * 데이터 없이 주석 줄만 내보낸다.
     *
     * <p>끊긴 연결은 write 를 시도해야만 드러나기 때문에, 아무 일이 없어도 주기적으로 이걸 보내서
     * 죽은 연결을 찾아낸다. 브라우저의 {@code EventSource} 는 주석 줄을 이벤트로 치지 않아서
     * 화면에는 아무 영향이 없고, 중간 프록시가 조용한 연결을 끊는 것도 같이 막아 준다.
     */
    void ping() throws IOException {
        sendLock.lock();
        try {
            emitter.send(SseEmitter.event().comment("ping"));
        } finally {
            sendLock.unlock();
        }
    }

    /**
     * 이미 끝난 emitter 에 {@code complete()} 를 부르면 {@code IllegalStateException} 이 난다.
     * 여기까지 온 연결은 어차피 정리 대상이라 삼킨다.
     */
    void completeQuietly() {
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            log.debug("이미 종료된 sse 연결", e);
        }
    }
}
