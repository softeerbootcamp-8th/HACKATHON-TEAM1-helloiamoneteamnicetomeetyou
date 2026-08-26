package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.List;
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
@DisplayName("어드민 교환 조작이 참가자에게 보내는 알림")
class AdminExchangeServiceTest {

    private static final UUID REAL_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DUMMY_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ExchangeParticipantRepository exchangeParticipantRepository;

    @Mock
    private SseEventPublisher sseEventPublisher;

    @InjectMocks
    private AdminExchangeService adminExchangeService;

    private Exchange exchange;
    private ExchangeParticipant dummyParticipant;

    @BeforeEach
    void setUp() {
        exchange = mock(Exchange.class);
        given(exchange.getId()).willReturn(1L);
        given(exchange.getZone()).willReturn(null);

        User realUser = mock(User.class);
        given(realUser.getId()).willReturn(REAL_USER_ID);
        given(realUser.isAdminManaged()).willReturn(false);

        User dummyUser = mock(User.class);
        given(dummyUser.getId()).willReturn(DUMMY_USER_ID);
        given(dummyUser.isAdminManaged()).willReturn(true);

        dummyParticipant = mock(ExchangeParticipant.class);
        given(dummyParticipant.getExchange()).willReturn(exchange);
        given(dummyParticipant.getUser()).willReturn(dummyUser);

        ExchangeParticipant realParticipant = mock(ExchangeParticipant.class);
        given(realParticipant.getUser()).willReturn(realUser);

        given(exchangeParticipantRepository.findById(10L)).willReturn(java.util.Optional.of(dummyParticipant));
        given(exchangeParticipantRepository.findAllByExchangeId(1L))
                .willReturn(List.of(realParticipant, dummyParticipant));
        given(exchangeRepository.findById(1L)).willReturn(java.util.Optional.of(exchange));
    }

    @Test
    void 더미_대신_수락하면_실제_참가자에게만_개인_알림을_보낸다() {
        adminExchangeService.acceptAsDummy(10L);

        verify(sseEventPublisher).toUser(eq(REAL_USER_ID), eq(SseEventType.MATCH_ACCEPTED), any());
        verify(sseEventPublisher, never()).toUser(eq(DUMMY_USER_ID), any(), any());
    }

    @Test
    void 더미_대신_거절하면_실제_참가자에게만_개인_알림을_보낸다() {
        adminExchangeService.rejectAsDummy(10L);

        verify(sseEventPublisher).toUser(eq(REAL_USER_ID), eq(SseEventType.MATCH_REJECTED), any());
        verify(sseEventPublisher, never()).toUser(eq(DUMMY_USER_ID), any(), any());
    }

    @Test
    void 교환을_취소하면_실제_참가자에게_개인_알림을_보낸다() {
        adminExchangeService.cancel(1L);

        verify(sseEventPublisher).toUser(eq(REAL_USER_ID), eq(SseEventType.EXCHANGE_CANCELLED), any());
    }

    @Test
    void 교환을_완료하면_본인이_한_행동이라_개인_알림을_보내지_않는다() {
        adminExchangeService.complete(1L);

        verify(sseEventPublisher, never()).toUser(any(), any(), any());
    }
}
