package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.entity.PushSubscription;
import com.interaso.webpush.WebPush;
import com.interaso.webpush.WebPushService;
import com.interaso.webpush.WebPushStatusException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 한 사용자의 모든 기기로 알림을 보낸다.
 *
 * <p>보낼지 말지는 여기서 정하지 않는다. {@link PushEventDispatcher} 가 앱이 열려 있는지 보고
 * 정한 뒤에 부른다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushSendService {

    private final WebPushService webPushService;
    private final PushSubscriptionService pushSubscriptionService;
    private final ObjectMapper objectMapper;

    public void send(UUID userId, PushMessage message) {
        send(userId, message.getTitle(), message.getBody(), message.getUrl());
    }

    public void send(UUID userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionService.findAllByUserId(userId);

        if (subscriptions.isEmpty()) {
            return;
        }

        String payload = toPayload(title, body, url);

        // 엔티티를 그대로 들고 다니지 않는다. 아래 HTTP 구간이 길고, 그 사이 지연 로딩이
        // 열릴 일이 없어야 한다(open-in-view=false).
        for (PushSubscription subscription : subscriptions) {
            sendOne(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
        }
    }

    private void sendOne(String endpoint, String p256dh, String auth, String payload) {
        try {
            // Kotlin 의 기본 인자는 자바에서 못 쓴다. ttl 과 topic 까지 전부 넘겨야 한다.
            WebPush.SubscriptionState state =
                    webPushService.send(payload, endpoint, p256dh, auth, null, null, WebPush.Urgency.High);

            if (state == WebPush.SubscriptionState.EXPIRED) {
                // 404/410. 브라우저가 구독을 지웠거나 사용자가 앱을 삭제했다. 다시 보낼 일이 없다.
                log.info("만료된 구독을 지운다: endpoint={}", endpoint);
                pushSubscriptionService.deleteByEndpoint(endpoint);
                return;
            }

            // 성공도 남긴다. 이게 없으면 "안 보냈다" 와 "보냈는데 화면에 안 떴다" 를 구분할 수 없어서,
            // 알림이 안 온다는 이야기가 나왔을 때 어디부터 봐야 할지 알 수 없다.
            log.info("푸시를 푸시 서비스에 넘겼다: endpoint={}", endpoint);
        } catch (Exception e) {
            // WebPushStatusException 은 코틀린에서 온 checked 예외인데 send() 에 throws 절이 없어서
            // 자바에서는 직접 catch 할 수 없다(잡을 수 없는 예외라고 컴파일이 막는다).
            // 그래서 Exception 으로 받아 여기서 갈라 본다.
            if (e instanceof WebPushStatusException statusException) {
                // 401/403 은 구독이 아니라 우리 VAPID 설정 문제다. 502/503 은 일시 장애다.
                // 둘 다 구독을 지우면 안 된다.
                log.error("푸시 발송 실패: status={}, endpoint={}",
                        statusException.getStatusCode(), endpoint, statusException);
                return;
            }

            log.error("푸시 발송 실패: endpoint={}", endpoint, e);
        }
    }

    /** Boot 4 가 쓰는 Jackson 3 는 직렬화 실패를 unchecked 로 던진다. 여기서 잡을 것이 없다. */
    private String toPayload(String title, String body, String url) {
        return objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", url));
    }
}
