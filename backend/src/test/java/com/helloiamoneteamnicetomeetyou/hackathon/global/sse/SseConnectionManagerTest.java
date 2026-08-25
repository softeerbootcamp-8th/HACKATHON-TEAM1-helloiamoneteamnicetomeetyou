package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SSE 연결 보관")
class SseConnectionManagerTest {

    private static final long TIMEOUT_MS = 60_000;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final SseConnectionManager connectionManager = new SseConnectionManager(executor, TIMEOUT_MS);

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    @DisplayName("한 사용자가 탭을 두 개 열면 연결도 두 개다")
    void 같은_사용자의_연결을_따로_센다() {
        UUID userId = UUID.randomUUID();

        connectionManager.subscribe(1L, userId);
        connectionManager.subscribe(1L, userId);

        assertThat(connectionManager.countByZone(1L)).isEqualTo(2);
        assertThat(connectionManager.findByUser(userId)).hasSize(2);
    }

    @Test
    @DisplayName("대기장소가 다르면 서로의 연결을 보지 않는다")
    void 대기장소별로_나뉜다() {
        connectionManager.subscribe(1L, UUID.randomUUID());
        connectionManager.subscribe(2L, UUID.randomUUID());

        assertThat(connectionManager.countByZone(1L)).isEqualTo(1);
        assertThat(connectionManager.countByZone(2L)).isEqualTo(1);
        assertThat(connectionManager.findByZone(3L)).isEmpty();
    }

    /**
     * 이게 없으면 배포할 때마다 종료가 30초씩 밀린다. 톰캣이 SSE 를 "아직 처리 중인 요청" 으로
     * 보고 끝나기를 기다리기 때문인데, 실측으로 확인하고 넣은 동작이라 테스트로 묶어 둔다.
     */
    @Test
    @DisplayName("서버가 내려갈 때 열려 있는 연결을 모두 닫는다")
    void 종료할_때_전부_닫는다() {
        connectionManager.subscribe(1L, UUID.randomUUID());
        connectionManager.subscribe(1L, UUID.randomUUID());
        connectionManager.subscribe(2L, UUID.randomUUID());

        connectionManager.closeAllOnShutdown();

        assertThat(connectionManager.countByZone(1L)).isZero();
        assertThat(connectionManager.countByZone(2L)).isZero();
    }
}
