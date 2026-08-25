package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseConnectionManager;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("앱이 닫혀 있을 때만 푸시를 보낸다")
class PushEventDispatcherTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Long BOOTH_ID = 1L;

    @Mock
    private SseConnectionManager connectionManager;

    @Mock
    private PushSendService pushSendService;

    @InjectMocks
    private PushEventDispatcher pushEventDispatcher;

    @Test
    @DisplayName("SSE 연결이 없으면 보낸다")
    void 앱이_닫혀_있으면_푸시를_보낸다() {
        given(connectionManager.hasConnection(USER_ID)).willReturn(false);

        pushEventDispatcher.onSseEvent(
                SseEvent.toUser(USER_ID, SseEventType.MATCH_SUGGESTED, null));

        verify(pushSendService).send(USER_ID, PushMessage.MATCH_SUGGESTED);
    }

    @Test
    @DisplayName("SSE 연결이 있으면 화면이 이미 받았으므로 보내지 않는다")
    void 앱이_열려_있으면_푸시를_보내지_않는다() {
        given(connectionManager.hasConnection(USER_ID)).willReturn(true);

        pushEventDispatcher.onSseEvent(
                SseEvent.toUser(USER_ID, SseEventType.MATCH_SUGGESTED, null));

        verify(pushSendService, never()).send(any(UUID.class), any(PushMessage.class));
    }

    @Test
    @DisplayName("부스 전체에 뿌리는 이벤트는 푸시하지 않는다")
    void 부스_브로드캐스트는_푸시하지_않는다() {
        pushEventDispatcher.onSseEvent(
                SseEvent.toBooth(BOOTH_ID, SseEventType.USER_JOINED, null));

        verify(pushSendService, never()).send(any(UUID.class), any(PushMessage.class));
    }

    @Test
    @DisplayName("알릴 내용이 없는 이벤트는 푸시하지 않는다")
    void 문구가_없는_이벤트는_푸시하지_않는다() {
        pushEventDispatcher.onSseEvent(SseEvent.toUser(USER_ID, SseEventType.CONNECTED, null));

        verify(pushSendService, never()).send(any(UUID.class), any(PushMessage.class));
    }

    /**
     * AFTER_COMMIT 리스너에서 새어 나간 예외는 커밋이 끝난 뒤 호출자에게 그대로 전파된다.
     * 그러면 교환은 저장됐는데 API 는 500 을 뱉는다. 이 테스트가 그걸 막는다.
     */
    @Test
    @DisplayName("발송이 실패해도 예외를 밖으로 내보내지 않는다")
    void 발송_실패가_거래를_망치지_않는다() {
        given(connectionManager.hasConnection(USER_ID)).willReturn(false);
        willThrow(new RuntimeException("푸시 서비스 장애"))
                .given(pushSendService).send(any(UUID.class), any(PushMessage.class));

        assertThatCode(() -> pushEventDispatcher.onSseEvent(
                SseEvent.toUser(USER_ID, SseEventType.MATCH_SUGGESTED, null)))
                .doesNotThrowAnyException();
    }
}
