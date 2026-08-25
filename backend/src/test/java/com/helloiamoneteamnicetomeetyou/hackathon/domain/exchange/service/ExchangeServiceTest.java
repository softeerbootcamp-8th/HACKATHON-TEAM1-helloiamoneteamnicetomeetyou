package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("교환 약속의 장소와 시간")
class ExchangeServiceTest {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long BOOTH_ID = 1L;
    private static final UUID ME = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PARTNER = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID OUTSIDER = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 8, 25, 14, 15);

    @Mock
    private ExchangeRepository exchangeRepository;
    @Mock
    private ExchangeParticipantRepository participantRepository;
    @Mock
    private ExchangeTimeSlotRepository timeSlotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private SseEventPublisher sseEventPublisher;

    @InjectMocks
    private ExchangeService exchangeService;

    private Exchange exchange;
    private User me;
    private User partner;

    @BeforeEach
    void setUp() throws Exception {
        Booth booth = withId(Booth.of("현대자동차 팝업", null), BOOTH_ID);
        Zone zone = withId(Zone.of(booth, "중앙 포토존 앞", "행사 중앙 포토존"), 1L);

        exchange = withId(Exchange.of(zone, ExchangeType.ONE_TO_ONE, BASE_TIME), EXCHANGE_ID);
        me = User.of(ME, "레몬 28");
        partner = User.of(PARTNER, "블루N");

        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        given(participantRepository.findAllByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeParticipant.of(exchange, me, 28), ExchangeParticipant.of(exchange, partner, 16)));
        given(timeSlotRepository.findAllByExchangeId(EXCHANGE_ID)).willReturn(List.of());
    }

    @Test
    @DisplayName("교환을 만들면 서버가 격자 시작점을 정한다")
    void 교환을_만들면_격자_시작점을_정한다() {
        Booth booth = exchange.getZone().getBooth();
        given(zoneRepository.findByBoothIdOrderByIdAsc(BOOTH_ID)).willReturn(List.of(exchange.getZone()));
        given(userRepository.findById(ME)).willReturn(Optional.of(me));
        given(userRepository.findById(PARTNER)).willReturn(Optional.of(partner));
        given(exchangeRepository.save(any(Exchange.class))).willReturn(exchange);

        ExchangeResponseDto response =
                exchangeService.create(BOOTH_ID, ExchangeType.ONE_TO_ONE, List.of(ME, PARTNER));

        assertThat(response.slotBaseTime()).isEqualTo(BASE_TIME);
        assertThat(response.slotCount()).isEqualTo(TimeSlotGrid.SLOT_COUNT);
        // 식별 화면에서 서로를 찾으려면 참가자마다 번호가 달라야 한다.
        assertThat(response.participants()).extracting(p -> p.identityNumber()).doesNotHaveDuplicates();
        assertThat(response.zone().name()).isEqualTo("중앙 포토존 앞");
        assertThat(response.boothId()).isEqualTo(booth.getId());
        verify(sseEventPublisher).toUser(eq(ME), eq(SseEventType.EXCHANGE_CREATED), any());
        verify(sseEventPublisher).toUser(eq(PARTNER), eq(SseEventType.EXCHANGE_CREATED), any());
    }

    @Test
    @DisplayName("참가자가 한 명이면 교환을 만들지 않는다")
    void 참가자가_한_명이면_막는다() {
        assertThatThrownBy(() -> exchangeService.create(BOOTH_ID, ExchangeType.ONE_TO_ONE, List.of(ME, ME)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_EXCHANGE_PARTICIPANTS);
    }

    @Test
    @DisplayName("시간을 저장하면 참가자 전원에게 알린다")
    void 시간을_저장하면_전원에게_알린다() {
        exchangeService.updateTimeSlots(EXCHANGE_ID, ME, List.of(2, 0, 2));

        verify(timeSlotRepository).deleteAllByExchangeIdAndUserId(EXCHANGE_ID, ME);
        verify(sseEventPublisher).toUser(eq(ME), eq(SseEventType.EXCHANGE_TIME_UPDATED), any());
        verify(sseEventPublisher).toUser(eq(PARTNER), eq(SseEventType.EXCHANGE_TIME_UPDATED), any());
    }

    @Test
    @DisplayName("참가자가 아니면 시간을 바꿀 수 없다")
    void 참가자가_아니면_막는다() {
        assertThatThrownBy(() -> exchangeService.updateTimeSlots(EXCHANGE_ID, OUTSIDER, List.of(0)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.NOT_EXCHANGE_PARTICIPANT);

        verify(timeSlotRepository, never()).deleteAllByExchangeIdAndUserId(anyLong(), any());
    }

    @Test
    @DisplayName("격자 밖의 칸은 저장하지 않는다")
    void 격자_밖의_칸은_저장하지_않는다() {
        assertThatThrownBy(() -> exchangeService.updateTimeSlots(EXCHANGE_ID, ME, List.of(TimeSlotGrid.SLOT_COUNT)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_TIME_SLOT);
    }

    @Test
    @DisplayName("겹치는 칸이 있으면 그중 가장 빠른 시각으로 확정한다")
    void 겹치는_가장_빠른_시각으로_확정한다() {
        given(timeSlotRepository.findAllByExchangeId(EXCHANGE_ID)).willReturn(List.of(
                ExchangeTimeSlot.of(exchange, me, 1),
                ExchangeTimeSlot.of(exchange, me, 3),
                ExchangeTimeSlot.of(exchange, partner, 3),
                ExchangeTimeSlot.of(exchange, partner, 1)));

        ExchangeResponseDto response = exchangeService.confirmTime(EXCHANGE_ID, ME);

        assertThat(response.confirmedTime()).isEqualTo(BASE_TIME.plusMinutes(TimeSlotGrid.SLOT_MINUTES));
        assertThat(response.overlapSlot()).isEqualTo(1);
        assertThat(response.allAnswered()).isTrue();
    }

    @Test
    @DisplayName("겹치는 칸이 없으면 확정하지 않는다")
    void 겹치는_칸이_없으면_확정하지_않는다() {
        given(timeSlotRepository.findAllByExchangeId(EXCHANGE_ID)).willReturn(List.of(
                ExchangeTimeSlot.of(exchange, me, 0),
                ExchangeTimeSlot.of(exchange, partner, 6)));

        assertThatThrownBy(() -> exchangeService.confirmTime(EXCHANGE_ID, ME))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.NO_OVERLAPPING_TIME);

        assertThat(exchange.getExchangeTime()).isNull();
    }

    @Test
    @DisplayName("이미 확정된 약속의 시간은 다시 바꿀 수 없다")
    void 이미_확정된_약속은_바꿀_수_없다() {
        exchange.confirmTime(2);

        assertThatThrownBy(() -> exchangeService.updateTimeSlots(EXCHANGE_ID, ME, List.of(0)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.EXCHANGE_TIME_ALREADY_CONFIRMED);
    }

    @Test
    @DisplayName("시간이 정해진 뒤에 도착을 알리면 전원에게 전해진다")
    void 도착을_알리면_전원에게_전해진다() {
        exchange.confirmTime(1);

        ExchangeResponseDto response = exchangeService.arrive(EXCHANGE_ID, ME);

        assertThat(response.participants())
                .filteredOn(p -> p.userId().equals(ME))
                .allMatch(ExchangeParticipantResponseDto::arrived);
        verify(sseEventPublisher).toUser(eq(PARTNER), eq(SseEventType.EXCHANGE_ARRIVED), any());
    }

    @Test
    @DisplayName("시간이 정해지기 전에는 도착을 받지 않는다")
    void 시간이_정해지기_전에는_도착을_받지_않는다() {
        assertThatThrownBy(() -> exchangeService.arrive(EXCHANGE_ID, ME))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.EXCHANGE_TIME_NOT_CONFIRMED);
    }

    @Test
    @DisplayName("시간 조율을 요청하면 전원의 선택을 비운다")
    void 시간_조율은_전원의_선택을_비운다() {
        exchangeService.resetTimeSlots(EXCHANGE_ID, ME);

        verify(timeSlotRepository).deleteAllByExchangeId(EXCHANGE_ID);
        assertThat(exchange.getExchangeTime()).isNull();
    }

    @Test
    @DisplayName("한 명만 골랐으면 아직 전원이 답한 것이 아니다")
    void 한_명만_골랐으면_전원이_답한_것이_아니다() {
        given(timeSlotRepository.findAllByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeTimeSlot.of(exchange, me, 0)));

        ExchangeResponseDto response = exchangeService.find(EXCHANGE_ID);

        assertThat(response.allAnswered()).isFalse();
        assertThat(response.overlapSlot()).isNull();
        assertThat(response.participants()).hasSize(2);
    }

    /** 저장 전 엔티티에 id 를 넣는다. 응답을 만들 때 id 가 필요한데 목 리포지토리는 채워 주지 않는다. */
    private static <T> T withId(T entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        return entity;
    }
}
