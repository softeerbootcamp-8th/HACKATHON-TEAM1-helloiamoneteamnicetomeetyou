package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("어드민이 더미 대신 시간 칸을 넣는다")
class AdminExchangeServiceTest {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long PARTICIPANT_ID = 10L;
    private static final UUID DUMMY = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID REAL = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Mock
    private ExchangeRepository exchangeRepository;
    @Mock
    private ExchangeParticipantRepository exchangeParticipantRepository;
    @Mock
    private ExchangeTimeSlotRepository exchangeTimeSlotRepository;
    @Mock
    private ExchangeService exchangeService;
    @Mock
    private SseEventPublisher sseEventPublisher;

    @InjectMocks
    private AdminExchangeService adminExchangeService;

    @Test
    @DisplayName("사용자 화면과 같은 경로로 보낸다")
    void 더미의_시간_칸은_교환_서비스를_그대로_부른다() {
        given(exchangeParticipantRepository.findById(PARTICIPANT_ID))
                .willReturn(Optional.of(participant(DUMMY, true)));

        adminExchangeService.updateTimeSlotsAsDummy(PARTICIPANT_ID, List.of(2, 5));

        verify(exchangeService).updateTimeSlots(EXCHANGE_ID, DUMMY, List.of(2, 5));
    }

    /**
     * 체크를 하나도 안 하면 폼이 {@code slots} 를 아예 안 보낸다. 그것을 빈 목록으로 다루지
     * 않으면 "고른 것 전부 지우기" 를 할 방법이 없어진다.
     */
    @Test
    @DisplayName("아무것도 안 고르면 전부 지우는 것으로 다룬다")
    void 슬롯이_없으면_빈_목록으로_넘긴다() {
        given(exchangeParticipantRepository.findById(PARTICIPANT_ID))
                .willReturn(Optional.of(participant(DUMMY, true)));

        adminExchangeService.updateTimeSlotsAsDummy(PARTICIPANT_ID, null);

        verify(exchangeService).updateTimeSlots(EXCHANGE_ID, DUMMY, List.of());
    }

    /**
     * 실제 참가자 줄은 그 사람이 자기 화면에서 고른 것이다. 운영자가 덮어쓰면 하지 않은 일이
     * 그 사람 이름으로 남는다. 화면에 폼을 안 그리는 것과 별개로 서버에서도 막는다.
     */
    /**
     * 예전에는 {@code participant.accept()} 만 해서 만날 자리도 시간 격자도 안 붙었다.
     * 교환이 "진행 중 · 장소 미정" 으로 남고 시간 격자가 뜨지 않던 원인이라 테스트로 고정한다.
     */
    @Test
    @DisplayName("수락은 사용자 화면과 같은 경로로 보내 약속까지 준비한다")
    void 수락은_교환_서비스를_그대로_부른다() {
        given(exchangeParticipantRepository.findById(PARTICIPANT_ID))
                .willReturn(Optional.of(participant(DUMMY, true)));

        adminExchangeService.acceptAsDummy(PARTICIPANT_ID);

        verify(exchangeService).accept(EXCHANGE_ID, DUMMY);
    }

    @Test
    @DisplayName("거절도 교환 서비스를 그대로 부른다")
    void 거절은_교환_서비스를_그대로_부른다() {
        given(exchangeParticipantRepository.findById(PARTICIPANT_ID))
                .willReturn(Optional.of(participant(DUMMY, true)));

        adminExchangeService.rejectAsDummy(PARTICIPANT_ID);

        verify(exchangeService).reject(EXCHANGE_ID, DUMMY);
    }

    @Test
    @DisplayName("도착도 더미 줄에만 열린다")
    void 도착은_더미만_찍을_수_있다() {
        given(exchangeParticipantRepository.findById(PARTICIPANT_ID))
                .willReturn(Optional.of(participant(REAL, false)));

        assertThatThrownBy(() -> adminExchangeService.arriveAsDummy(PARTICIPANT_ID))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorCode.NOT_EXCHANGE_PARTICIPANT);

        verify(exchangeService, never()).arrive(anyLong(), any());
    }

    @Test
    @DisplayName("실제 참가자 줄은 운영자가 덮어쓸 수 없다")
    void 더미가_아니면_막는다() {
        given(exchangeParticipantRepository.findById(PARTICIPANT_ID))
                .willReturn(Optional.of(participant(REAL, false)));

        assertThatThrownBy(() -> adminExchangeService.updateTimeSlotsAsDummy(PARTICIPANT_ID, List.of(1)))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorCode.NOT_EXCHANGE_PARTICIPANT);

        verify(exchangeService, never()).updateTimeSlots(anyLong(), any(), any());
    }

    private ExchangeParticipant participant(UUID userId, boolean dummy) {
        User user = dummy ? User.dummy(userId, "더미") : User.of(userId, "참가자");

        Exchange exchange = Exchange.create(ExchangeType.ONE_TO_ONE);
        setField(exchange, "id", EXCHANGE_ID);

        return ExchangeParticipant.accepted(exchange, user);
    }

    /** 엔티티의 id 는 저장할 때 붙는 값이라 테스트에서 직접 넣어 준다. */
    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
