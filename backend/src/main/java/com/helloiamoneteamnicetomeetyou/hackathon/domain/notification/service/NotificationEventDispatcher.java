package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.entity.Notification;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.repository.NotificationRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service.PushMessage;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 실시간으로 나가는 알림과 같은 내용을 저장해 둔다.
 *
 * <p>{@code PushEventDispatcher} 와 같은 이벤트를 듣고, 문구도 {@link PushMessage} 를 그대로
 * 재사용한다. 잠금 화면 문구와 알림함 문구가 따로 놀면 한쪽만 고쳤을 때 조용히 어긋난다.
 *
 * <p>덕분에 도메인 서비스는 손대지 않는다. {@code sseEventPublisher.toUser(...)} 하나만
 * 부르면, 화면이 보고 있으면 SSE 로 나가고 저장은 여기서 알아서 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventDispatcher {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSseEvent(SseEvent event) {
        // 부스 전체에 뿌리는 이벤트는 개인 알림함에 쌓을 대상이 없다.
        if (event.userId() == null) {
            return;
        }

        Optional<PushMessage> message = PushMessage.from(event.type());
        if (message.isEmpty()) {
            return;
        }

        try {
            // getReferenceById 와 save 를 같은 트랜잭션(영속성 컨텍스트)에서 실행한다. 원래
            // 트랜잭션은 이미 커밋되어 열려 있지 않으므로, 새로 하나 열지 않으면 두 호출이
            // 서로 다른 영속성 컨텍스트에서 실행돼 recipient 참조가 어느 세션에도 속하지 않는다.
            User recipient = userRepository.getReferenceById(event.userId());
            notificationRepository.save(Notification.of(
                    recipient, event.type(), message.get().getTitle(), message.get().getBody()));
        } catch (Exception e) {
            // AFTER_COMMIT 리스너의 예외는 커밋이 끝난 뒤 호출자에게 그대로 전파된다. 알림
            // 저장이 실패했다고 이미 끝난 매칭·교환 응답이 500 이 되면 안 된다.
            log.error("알림 저장 실패: userId={}, type={}", event.userId(), event.type(), e);
        }
    }
}
