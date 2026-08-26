package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeEventDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.repository.NotificationRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("알림 저장 리스너")
class NotificationEventDispatcherTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        given(userRepository.getReferenceById(USER_ID)).willReturn(mock(User.class));
    }

    @Test
    void 문구가_있는_사용자_대상_이벤트는_저장한다() {
        SseEvent event = SseEvent.toUser(USER_ID, SseEventType.POKE_RECEIVED, Map.of());

        dispatcher.onSseEvent(event);

        verify(notificationRepository).save(any());
    }

    @Test
    void 부스_전체에_뿌리는_이벤트는_저장하지_않는다() {
        SseEvent event = SseEvent.toBooth(1L, SseEventType.USER_JOINED, Map.of());

        dispatcher.onSseEvent(event);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void 알림_문구가_없는_이벤트는_저장하지_않는다() {
        // CONNECTED 는 연결 신호일 뿐이라 PushMessage 에 문구가 없다.
        SseEvent event = SseEvent.toUser(USER_ID, SseEventType.CONNECTED, Map.of());

        dispatcher.onSseEvent(event);

        verify(notificationRepository, never()).save(any());
    }

    /*
      여기부터가 "끝난 교환의 알림을 걷어낸다" 다.

      알림은 사람이 탭하거나 스와이프할 때만 사라진다. 그래서 약속이 끝나거나 취소돼도 중간
      단계 알림("시간 매칭에 실패했어요" 같은 것)이 대기 화면에 남고, 눌러 봐야 이미 없는
      약속 화면으로 간다. 교환이 끝나는 순간 그 교환 번호로 한 번에 읽음 처리한다.
    */

    @Test
    @DisplayName("교환이 끝나면 그 교환의 알림을 읽음 처리한다")
    void 교환이_끝나면_그_교환의_알림을_정리한다() {
        SseEvent event = SseEvent.toUser(
                USER_ID, SseEventType.EXCHANGE_COMPLETED, new ExchangeEventDto(7L));

        dispatcher.onSseEvent(event);

        verify(notificationRepository)
                .markAllReadByExchangeId(eq(7L), eq(SseEventType.EXCHANGE_COMPLETED));
        // 완료는 본인이 방금 한 행동이라 알림함에 쌓지 않는다. 그래도 정리는 해야 한다.
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("취소 알림은 저장하고 나머지 알림만 읽음 처리한다")
    void 취소_알림은_남기고_나머지를_정리한다() {
        SseEvent event = SseEvent.toUser(
                USER_ID, SseEventType.EXCHANGE_CANCELLED, new ExchangeEventDto(7L));

        dispatcher.onSseEvent(event);

        /*
          자기 자신을 지우지 않는 것이 중요하다. 셋이 하는 교환은 취소 이벤트가 사람 수만큼
          따로 발행되는데, 종류를 안 가리고 지우면 두 번째 발행이 첫 번째로 저장된 취소 알림을
          지워서 어떤 참가자는 취소됐다는 사실을 못 받는다.
        */
        verify(notificationRepository)
                .markAllReadByExchangeId(eq(7L), eq(SseEventType.EXCHANGE_CANCELLED));
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("매칭을 거절해도 그 교환의 알림을 읽음 처리한다")
    void 매칭을_거절하면_그_교환의_알림을_정리한다() {
        // 거절은 교환을 CANCELLED 로 만들지만 EXCHANGE_CANCELLED 를 보내지 않는다.
        SseEvent event = SseEvent.toUser(
                USER_ID, SseEventType.MATCH_REJECTED, new ExchangeEventDto(7L));

        dispatcher.onSseEvent(event);

        verify(notificationRepository)
                .markAllReadByExchangeId(eq(7L), eq(SseEventType.MATCH_REJECTED));
    }

    @Test
    @DisplayName("교환이 끝나는 이벤트가 아니면 아무것도 읽음 처리하지 않는다")
    void 진행_중인_이벤트는_정리하지_않는다() {
        SseEvent event = SseEvent.toUser(
                USER_ID, SseEventType.EXCHANGE_TIME_MISMATCHED, new ExchangeEventDto(7L));

        dispatcher.onSseEvent(event);

        verify(notificationRepository, never()).markAllReadByExchangeId(any(), any());
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("교환 번호가 없는 payload 면 정리를 건너뛴다")
    void 교환_번호가_없으면_정리하지_않는다() {
        // 찔러보기 수신은 아직 교환이 없다. 지울 대상을 특정할 수 없으니 건드리지 않는다.
        SseEvent event = SseEvent.toUser(USER_ID, SseEventType.POKE_RECEIVED, Map.of());

        dispatcher.onSseEvent(event);

        verify(notificationRepository, never()).markAllReadByExchangeId(any(), any());
    }
}
