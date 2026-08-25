package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service;

import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseConnectionManager;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 앱이 닫혀 있는 사람에게만 잠금 화면 알림을 보낸다.
 *
 * <p><b>실시간 알림과 같은 이벤트를 듣는다.</b> 도메인 서비스는 지금처럼
 * {@code sseEventPublisher.toUser(...)} 하나만 부르면 되고, 그 사람이 앱을 보고 있으면 SSE 로,
 * 아니면 푸시로 나간다. 어느 쪽으로 갈지는 여기서 갈리므로 부르는 쪽은 몰라도 된다.
 *
 * <p>덕분에 같은 소식이 두 번 가는 일이 없다. 클라이언트에서 "앱이 열려 있으면 무시" 로 막을
 * 수는 없는데, iOS 는 푸시를 받고 알림을 띄우지 않으면 구독을 해지해 버리기 때문이다.
 *
 * <p>{@code SseEventDispatcher} 를 고치지 않고 리스너를 따로 둔 것은, 실시간 전송 코드가 푸시를
 * 알 필요가 없어서다. 두 리스너의 실행 순서는 정해져 있지 않지만 서로 독립적이라 상관없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushEventDispatcher {

    private final SseConnectionManager connectionManager;
    private final PushSendService pushSendService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSseEvent(SseEvent event) {
        // 부스 전체에 뿌리는 이벤트는 푸시하지 않는다. 접속 알림 같은 것이 전원에게 가면 스팸이다.
        if (event.userId() == null) {
            return;
        }

        Optional<PushMessage> message = PushMessage.from(event.type());
        if (message.isEmpty()) {
            return;
        }

        // 앱을 보고 있으면 화면이 이미 받았다.
        if (connectionManager.hasConnection(event.userId())) {
            return;
        }

        try {
            pushSendService.send(event.userId(), message.get());
        } catch (Exception e) {
            // AFTER_COMMIT 리스너에서 나간 예외는 커밋이 끝난 뒤 호출자에게 그대로 전파된다.
            // 그대로 두면 교환은 저장됐는데 API 는 500 을 뱉는다. 알림 실패가 거래를 망치면 안 된다.
            log.error("푸시 발송 실패: userId={}, type={}", event.userId(), event.type(), e);
        }
    }
}
