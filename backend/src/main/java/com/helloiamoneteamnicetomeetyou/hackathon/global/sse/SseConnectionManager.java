package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 열려 있는 SSE 연결을 서버 메모리에 들고 있으면서, 부스나 사용자 단위로 찾아서 내보낸다.
 *
 * <p><b>연결 단위는 부스다.</b> 같은 부스에 들어와 있는 사람들이 서로의 접속과 매칭을 실시간으로
 * 봐야 하기 때문이다. 구역({@code Zone})은 지도에서 고르는 만나는 자리라 사람이 옮겨 다닐 수
 * 있고, 그때마다 연결을 끊었다 다시 맺으면 그 사이 이벤트를 놓친다.
 *
 * <p>연결을 메모리에 두기 때문에 <b>이 서버에 붙은 연결만</b> 안다. 지금은 EC2 한 대라 그것이
 * 전부라서 문제가 없지만, 서버를 늘리면 다른 인스턴스에 붙은 사람은 이벤트를 못 받는다. 그때는
 * 인스턴스 사이에 이벤트를 퍼뜨릴 방법을 팀에서 정해야 한다.
 *
 * <p>부스와 사용자 두 벌의 색인을 유지한다. 매칭 제안처럼 특정 사용자에게만 보내야 하는 이벤트가
 * 있는데, 그때 전체 연결을 훑지 않으려면 사용자 색인이 필요하다.
 */
@Slf4j
@Component
public class SseConnectionManager {

    private final Map<Long, Set<SseConnection>> boothConnections = new ConcurrentHashMap<>();
    private final Map<UUID, Set<SseConnection>> userConnections = new ConcurrentHashMap<>();

    private final ExecutorService sseExecutor;
    private final long emitterTimeoutMs;

    public SseConnectionManager(
            @Qualifier("sseExecutor") ExecutorService sseExecutor,
            @Value("${sse.emitter-timeout-ms}") long emitterTimeoutMs) {
        this.sseExecutor = sseExecutor;
        this.emitterTimeoutMs = emitterTimeoutMs;
    }

    /**
     * 부스를 구독한다.
     *
     * <p><b>순서가 중요하다.</b> 색인에 넣고 생명주기 콜백을 건 다음에 {@code CONNECTED} 를
     * 보낸다. 콜백을 걸기 전에 전송하다 실패하면 그 연결이 색인에 남은 채로 아무도 치우지
     * 않는다.
     *
     * <p>{@code CONNECTED} 는 이 자리에서 바로 보낸다. 첫 바이트가 나가야 브라우저가 연결이
     * 열린 것으로 보기 때문에, 이걸 미루면 화면이 한동안 연결 중 상태에 머문다.
     */
    public SseEmitter subscribe(Long boothId, UUID userId) {
        SseConnection connection = new SseConnection(boothId, userId, new SseEmitter(emitterTimeoutMs));

        register(connection);

        SseEmitter emitter = connection.getEmitter();
        emitter.onCompletion(() -> disconnect(connection, "완료"));
        emitter.onTimeout(() -> disconnect(connection, "타임아웃"));
        emitter.onError(e -> disconnect(connection, "전송 오류"));

        try {
            connection.send(SseEventType.CONNECTED, Map.of("boothId", boothId, "userId", userId.toString()));
        } catch (Exception e) {
            log.debug("sse 연결 직후 전송 실패: boothId={}, userId={}", boothId, userId, e);
            drop(connection, "연결 직후 전송 실패");
        }

        log.info("sse 연결: boothId={}, userId={}, 이 부스 연결 수={}", boothId, userId, countByBooth(boothId));

        return emitter;
    }

    /** 부스에 붙어 있는 연결. 없으면 빈 목록이다. */
    Collection<SseConnection> findByBooth(Long boothId) {
        return boothConnections.getOrDefault(boothId, Set.of());
    }

    /** 한 사용자가 열어 둔 연결. 탭을 여러 개 열었으면 여러 개다. */
    Collection<SseConnection> findByUser(UUID userId) {
        return userConnections.getOrDefault(userId, Set.of());
    }

    public int countByBooth(Long boothId) {
        return findByBooth(boothId).size();
    }

    /**
     * 이 사용자가 지금 앱을 열어 두고 있는지.
     *
     * <p>웹푸시가 이걸 본다. 연결이 있으면 화면이 실시간으로 받고 있다는 뜻이라 잠금 화면 알림까지
     * 보내면 같은 소식이 두 번 간다. iOS 는 푸시를 받으면 반드시 알림을 띄워야 해서 클라이언트
     * 쪽에서 억누를 수가 없고, 그래서 보낼지 말지를 서버가 여기서 정한다.
     */
    public boolean hasConnection(UUID userId) {
        return !findByUser(userId).isEmpty();
    }

    /**
     * 연결마다 전송을 {@code sseExecutor} 에 넘긴다. 부르는 스레드는 바로 돌아온다.
     *
     * <p><b>여기서 직접 write 하면 안 된다.</b> 전파가 안 되는 곳에 있는 사람 한 명이 소켓
     * write 에서 멈추면, 같은 부스의 나머지 전송이 전부 그 뒤에 줄을 서게 된다.
     *
     * <p>연결마다 스레드가 달라 두 이벤트의 도착 순서는 보장하지 않는다. 화면이 이벤트 순서에
     * 기대지 않고 알림을 받으면 현재 상태를 다시 읽는 방식이라 문제가 되지 않는다. 같은 연결에
     * 두 전송이 겹쳐 바이트가 섞이는 것은 {@link SseConnection} 의 락이 막는다.
     */
    void dispatch(Collection<SseConnection> targets, SseEventType type, Object data) {
        for (SseConnection connection : targets) {
            sseExecutor.execute(() -> {
                try {
                    connection.send(type, data);
                } catch (Exception e) {
                    log.debug("sse 전송 실패, 연결을 정리한다: boothId={}, type={}",
                            connection.getBoothId(), type, e);
                    drop(connection, "전송 실패");
                }
            });
        }
    }

    /**
     * 살아 있는 연결에 주기적으로 주석 줄을 보낸다.
     *
     * <p>끊긴 연결은 write 를 시도할 때만 드러나기 때문에, 이게 없으면 브라우저를 닫고 간 사람의
     * 연결이 emitter 타임아웃까지 색인에 남는다. 전송 실패는 {@link #dispatch} 와 같은 경로로
     * 정리된다.
     */
    @Scheduled(fixedRateString = "${sse.heartbeat-interval-ms}")
    void sendHeartbeat() {
        List<SseConnection> all = boothConnections.values().stream().flatMap(Set::stream).toList();

        for (SseConnection connection : all) {
            sseExecutor.execute(() -> {
                try {
                    connection.ping();
                } catch (Exception e) {
                    log.debug("sse heartbeat 실패, 연결을 정리한다: boothId={}", connection.getBoothId(), e);
                    drop(connection, "heartbeat 실패");
                }
            });
        }
    }

    /**
     * 서버가 내려가기 시작할 때 열려 있는 연결을 먼저 닫는다.
     *
     * <p><b>없으면 배포할 때마다 종료가 30초씩 밀린다.</b> SSE 는 끝나지 않는 요청이라 톰캣이
     * 보기에는 계속 처리 중인 async 요청이고, 종료 신호를 받아도 그것들이 끝나기를 기다리다
     * 타임아웃이 나야 죽는다. 실측으로 확인했다(연결 3개, SIGTERM 이후 30초).
     * EC2 배포는 {@code docker stop} 으로 컨테이너를 바꾸기 때문에 그 시간이 그대로 배포 지연이
     * 되고, 그 사이 붙어 있던 사람들은 끊긴 줄도 모른 채 조용히 기다린다.
     *
     * <p>{@code @PreDestroy} 가 아니라 {@link ContextClosedEvent} 인 것이 중요하다. 빈 소멸은
     * 톰캣이 요청을 기다린 <b>뒤에</b> 일어나서, 거기서 닫으면 이미 30초를 다 쓴 다음이다.
     * 이 이벤트는 그 기다림이 시작되기 전에 온다.
     *
     * <p>연결마다 종료 로그를 남기지 않는다. 접속자가 많을 때 종료 로그만 수백 줄이 되고,
     * 개별 사유가 전부 "서버 종료"로 같아서 알 수 있는 것이 없다.
     */
    @EventListener(ContextClosedEvent.class)
    void closeAllOnShutdown() {
        List<SseConnection> all = boothConnections.values().stream().flatMap(Set::stream).toList();

        if (all.isEmpty()) {
            return;
        }

        log.info("서버 종료: 열려 있는 sse 연결 {}개를 닫는다", all.size());

        for (SseConnection connection : all) {
            unregister(connection);
            connection.completeQuietly();
        }
    }

    private void register(SseConnection connection) {
        boothConnections
                .computeIfAbsent(connection.getBoothId(), id -> ConcurrentHashMap.newKeySet())
                .add(connection);
        userConnections
                .computeIfAbsent(connection.getUserId(), id -> ConcurrentHashMap.newKeySet())
                .add(connection);
    }

    /**
     * 색인에서 걷어내고, 그 부스나 사용자의 마지막 연결이었으면 항목 자체를 지운다.
     *
     * <p><b>{@code get} 으로 꺼내 비었는지 보고 {@code remove} 하면 안 된다.</b> 비었는지 확인한
     * 뒤 지우기 전에 다른 스레드가 그 부스에 새로 붙으면, 그 연결이 든 Set 을 통째로 버려서
     * 방금 들어온 사람이 아무 이벤트도 못 받는다. {@code compute} 는 키 하나에 대해 원자적이라
     * 그 틈이 없다.
     *
     * <p>실제로 뭔가를 지웠을 때만 {@code true} 다. 같은 연결에 {@code onError} 와
     * {@code onCompletion} 이 겹쳐 불려도 로그가 두 번 찍히지 않는다.
     */
    private boolean unregister(SseConnection connection) {
        AtomicBoolean removed = new AtomicBoolean();

        boothConnections.compute(connection.getBoothId(), (id, connections) -> {
            if (connections == null) {
                return null;
            }
            removed.set(connections.remove(connection));
            return connections.isEmpty() ? null : connections;
        });

        userConnections.compute(connection.getUserId(), (id, connections) -> {
            if (connections == null) {
                return null;
            }
            connections.remove(connection);
            return connections.isEmpty() ? null : connections;
        });

        return removed.get();
    }

    /** emitter 콜백이 알려준 종료. 이미 닫힌 뒤라 {@code complete()} 를 부르지 않는다. */
    private void disconnect(SseConnection connection, String reason) {
        if (unregister(connection)) {
            log.info("sse 연결 종료: boothId={}, userId={}, 사유={}",
                    connection.getBoothId(), connection.getUserId(), reason);
        }
    }

    /**
     * 전송이 실패해서 우리 쪽에서 걷어내는 경우다.
     *
     * <p>색인에서 빼는 것만으로는 부족하다. HTTP 연결은 아직 살아 있고 브라우저의
     * {@code EventSource} 가 자동으로 다시 붙기 때문에, 명시적으로 닫아 줘야 재연결이 돈다.
     * {@code complete()} 는 {@code onCompletion} 을 부르고 그쪽에서 {@code disconnect} 가 한 번
     * 더 돌지만, 이미 지운 뒤라 아무 일도 하지 않는다. 종료 로그가 두 번 찍히지 않도록 여기서
     * 먼저 {@code disconnect} 를 부른다.
     */
    private void drop(SseConnection connection, String reason) {
        disconnect(connection, reason);
        connection.completeQuietly();
    }
}
