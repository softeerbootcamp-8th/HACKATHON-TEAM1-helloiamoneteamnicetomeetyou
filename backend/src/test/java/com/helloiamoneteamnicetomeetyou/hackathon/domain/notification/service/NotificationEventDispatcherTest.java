package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
