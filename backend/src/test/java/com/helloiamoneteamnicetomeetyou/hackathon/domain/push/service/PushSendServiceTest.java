package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.entity.PushSubscription;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.interaso.webpush.WebPush;
import com.interaso.webpush.WebPushService;
import com.interaso.webpush.WebPushStatusException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("푸시 발송")
class PushSendServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String ENDPOINT = "https://web.push.apple.com/abc123";

    @Mock
    private WebPushService webPushService;

    @Mock
    private PushSubscriptionService pushSubscriptionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PushSendService pushSendService;

    @Test
    @DisplayName("만료된 구독은 지운다")
    void 만료된_구독은_지운다() {
        given(pushSubscriptionService.findAllByUserId(USER_ID)).willReturn(List.of(subscription()));
        given(webPushService.send(anyString(), anyString(), anyString(), anyString(), any(), any(), any()))
                .willReturn(WebPush.SubscriptionState.EXPIRED);

        pushSendService.send(USER_ID, PushMessage.MATCH_SUGGESTED);

        verify(pushSubscriptionService).deleteByEndpoint(ENDPOINT);
    }

    @Test
    @DisplayName("살아 있는 구독은 지우지 않는다")
    void 정상_발송이면_구독을_유지한다() {
        given(pushSubscriptionService.findAllByUserId(USER_ID)).willReturn(List.of(subscription()));
        given(webPushService.send(anyString(), anyString(), anyString(), anyString(), any(), any(), any()))
                .willReturn(WebPush.SubscriptionState.ACTIVE);

        pushSendService.send(USER_ID, PushMessage.MATCH_SUGGESTED);

        verify(pushSubscriptionService, never()).deleteByEndpoint(anyString());
    }

    /**
     * 403 은 구독이 죽은 것이 아니라 우리 VAPID 설정이 틀렸다는 뜻이다. 여기서 구독을 지우면
     * 설정을 고친 뒤에도 사용자가 알림을 다시 켜야 한다.
     */
    @Test
    @DisplayName("인증 실패(403)는 우리 설정 문제라 구독을 지우지 않는다")
    void 인증_실패는_구독을_지우지_않는다() {
        given(pushSubscriptionService.findAllByUserId(USER_ID)).willReturn(List.of(subscription()));
        // WebPushStatusException 은 코틀린에서 온 checked 예외라 willThrow 가 거부한다
        // (send() 에 throws 절이 없어서 Mockito 가 "던질 수 없는 예외"로 본다). Answer 로 직접 던진다.
        given(webPushService.send(anyString(), anyString(), anyString(), anyString(), any(), any(), any()))
                .willAnswer(invocation -> {
                    throw new WebPushStatusException(403, "Forbidden", null);
                });

        pushSendService.send(USER_ID, PushMessage.MATCH_SUGGESTED);

        verify(pushSubscriptionService, never()).deleteByEndpoint(anyString());
    }

    @Test
    @DisplayName("구독이 없으면 발송을 시도하지 않는다")
    void 구독이_없으면_아무것도_하지_않는다() {
        given(pushSubscriptionService.findAllByUserId(USER_ID)).willReturn(List.of());

        pushSendService.send(USER_ID, PushMessage.MATCH_SUGGESTED);

        verify(webPushService, never())
                .send(anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("한 기기가 실패해도 나머지 기기로는 보낸다")
    void 한_기기의_실패가_다른_기기를_막지_않는다() {
        PushSubscription second = PushSubscription.of(User.of(USER_ID), "https://fcm.googleapis.com/xyz", "p2", "a2");
        given(pushSubscriptionService.findAllByUserId(USER_ID))
                .willReturn(List.of(subscription(), second));
        given(webPushService.send(anyString(), eq(ENDPOINT), anyString(), anyString(), any(), any(), any()))
                .willAnswer(invocation -> {
                    throw new WebPushStatusException(500, "Server Error", null);
                });
        given(webPushService.send(anyString(), eq("https://fcm.googleapis.com/xyz"), anyString(),
                anyString(), any(), any(), any()))
                .willReturn(WebPush.SubscriptionState.ACTIVE);

        pushSendService.send(USER_ID, PushMessage.MATCH_SUGGESTED);

        verify(webPushService).send(anyString(), eq("https://fcm.googleapis.com/xyz"), anyString(),
                anyString(), any(), any(), any());
    }

    private static PushSubscription subscription() {
        return PushSubscription.of(User.of(USER_ID), ENDPOINT, "p256dh-value", "auth-value");
    }
}
