package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.entity.Notification;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.repository.NotificationRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorType;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.List;
import java.util.Optional;
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

/** 엔티티를 mock 으로 만드는 것은 {@code id} 가 DB 시퀀스라 정적 팩토리로 채울 수 없어서다. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("알림함")
class NotificationServiceTest {

    private static final UUID RECIPIENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Long NOTIFICATION_ID = 5L;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;
    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = mock(User.class);
        given(recipient.getId()).willReturn(RECIPIENT);

        notification = mock(Notification.class);
        given(notification.getId()).willReturn(NOTIFICATION_ID);
        given(notification.getRecipient()).willReturn(recipient);
        given(notification.getType()).willReturn(SseEventType.POKE_RECEIVED);
        given(notification.getTitle()).willReturn("교환 신청이 왔어요~");
        given(notification.getBody()).willReturn("탭하여 확인해 보세요");
    }

    @Test
    void 내_알림을_최근순으로_조회한다() {
        given(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(RECIPIENT))
                .willReturn(List.of(notification));

        PageResponse<?> result = notificationService.findAll(RECIPIENT, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 사용자_없이_조회하면_실패한다() {
        assertThatThrownBy(() -> notificationService.findAll(null, 0, 20))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 받은_사람이_읽음_처리하면_성공한다() {
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

        notificationService.markAsRead(NOTIFICATION_ID, RECIPIENT);

        verify(notification).markAsRead();
    }

    @Test
    void 받은_사람이_아니면_읽음_처리할_수_없다() {
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, OTHER))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_RECIPIENT);

        verify(notification, never()).markAsRead();
    }

    @Test
    void 없는_알림을_읽음_처리하면_실패한다() {
        given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, RECIPIENT))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
