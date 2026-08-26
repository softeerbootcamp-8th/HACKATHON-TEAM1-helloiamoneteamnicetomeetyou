package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.ItemStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import org.springframework.context.ApplicationEventPublisher;
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
    private static final Long ITEM_ID = 10L;
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
    private ExchangeItemRepository exchangeItemRepository;
    @Mock
    private UserHaveItemRepository userHaveItemRepository;
    @Mock
    private UserWantItemRepository userWantItemRepository;
    @Mock
    private SseEventPublisher sseEventPublisher;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ExchangeService exchangeService;

    private Exchange exchange;
    private User me;
    private User partner;

    @BeforeEach
    void setUp() throws Exception {
        Booth booth = withId(Booth.of("현대자동차 팝업", null), BOOTH_ID);
        Zone zone = withId(Zone.of(booth, "중앙 포토존 앞", "행사 중앙 포토존"), 1L);

        exchange = withId(Exchange.create(ExchangeType.ONE_TO_ONE), EXCHANGE_ID);
        // 만날 자리와 격자, 식별자는 참가자가 장소를 잡으러 들어올 때 붙는다.
        exchange.prepareAppointment(zone, BASE_TIME, 2, 28);
        me = User.of(ME, "레몬 28");
        partner = User.of(PARTNER, "블루N");

        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        given(participantRepository.findAllByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeParticipant.accepted(exchange, me), ExchangeParticipant.accepted(exchange, partner)));
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
        given(exchangeRepository.findIdentityCodesByStatuses(any())).willReturn(List.of());

        ExchangeResponseDto response =
                toResponseOf(exchangeService.createExchange(BOOTH_ID, ExchangeType.ONE_TO_ONE, List.of(ME, PARTNER)));

        assertThat(response.slotBaseTime()).isEqualTo(BASE_TIME);
        assertThat(response.slotCount()).isEqualTo(TimeSlotGrid.SLOT_COUNT);
        // 식별자는 교환이 통째로 갖는다. 참가자가 같은 화면을 들어야 서로를 찾을 수 있다.
        assertThat(response.identityNumber()).isBetween(10, 99);
        assertThat(response.zone().name()).isEqualTo("중앙 포토존 앞");
        assertThat(response.boothId()).isEqualTo(booth.getId());
        verify(sseEventPublisher).toUser(eq(ME), eq(SseEventType.EXCHANGE_CREATED), any());
        verify(sseEventPublisher).toUser(eq(PARTNER), eq(SseEventType.EXCHANGE_CREATED), any());
    }

    @Test
    @DisplayName("진행 중인 교환이 쓰는 식별자는 피해서 고른다")
    void 쓰이고_있는_식별자는_피한다() {
        given(zoneRepository.findByBoothIdOrderByIdAsc(BOOTH_ID)).willReturn(List.of(exchange.getZone()));
        given(userRepository.findById(ME)).willReturn(Optional.of(me));
        given(userRepository.findById(PARTNER)).willReturn(Optional.of(partner));
        given(exchangeRepository.save(any(Exchange.class))).willReturn(exchange);

        // 교환 id 1 이 처음 집으려는 자리를 미리 차지해 둔다.
        int taken = firstCandidateCode(EXCHANGE_ID);
        given(exchangeRepository.findIdentityCodesByStatuses(any())).willReturn(List.of(taken));

        ExchangeResponseDto response =
                toResponseOf(exchangeService.createExchange(BOOTH_ID, ExchangeType.ONE_TO_ONE, List.of(ME, PARTNER)));

        assertThat(response.identityMark() * 100 + response.identityNumber()).isNotEqualTo(taken);
    }

    /** 서비스가 비어 있을 때 고르는 첫 자리. 배정 규칙과 같은 식이라 규칙이 바뀌면 같이 바뀐다. */
    private static int firstCandidateCode(long exchangeId) {
        int start = Math.floorMod(exchangeId * 37, 8 * 90);
        return (start / 90) * 100 + 10 + start % 90;
    }

    @Test
    @DisplayName("참가자가 한 명이면 교환을 만들지 않는다")
    void 참가자가_한_명이면_막는다() {
        assertThatThrownBy(() -> exchangeService.createExchange(BOOTH_ID, ExchangeType.ONE_TO_ONE, List.of(ME, ME)))
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
    @DisplayName("자리를 옮기면 저장하고 참가자 전원에게 알린다")
    void 자리를_옮기면_전원에게_알린다() throws Exception {
        Booth booth = exchange.getZone().getBooth();
        Zone lounge = withId(Zone.of(booth, "라운지", "2층 라운지"), 2L);
        given(zoneRepository.findById(2L)).willReturn(Optional.of(lounge));

        exchangeService.updateZone(EXCHANGE_ID, ME, 2L);

        assertThat(exchange.getZone()).isEqualTo(lounge);
        verify(sseEventPublisher).toUser(eq(ME), eq(SseEventType.EXCHANGE_PLACE_UPDATED), any());
        verify(sseEventPublisher).toUser(eq(PARTNER), eq(SseEventType.EXCHANGE_PLACE_UPDATED), any());
    }

    @Test
    @DisplayName("참가자가 아니면 자리를 옮길 수 없다")
    void 참가자가_아니면_자리를_못_옮긴다() {
        assertThatThrownBy(() -> exchangeService.updateZone(EXCHANGE_ID, OUTSIDER, 2L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.NOT_EXCHANGE_PARTICIPANT);
    }

    @Test
    @DisplayName("다른 부스의 자리로는 옮길 수 없다")
    void 다른_부스의_자리로는_못_옮긴다() throws Exception {
        Booth otherBooth = withId(Booth.of("다른 팝업", null), 99L);
        Zone otherZone = withId(Zone.of(otherBooth, "남의 부스 자리", "저쪽"), 3L);
        given(zoneRepository.findById(3L)).willReturn(Optional.of(otherZone));

        assertThatThrownBy(() -> exchangeService.updateZone(EXCHANGE_ID, ME, 3L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.ZONE_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 자리로는 옮길 수 없다")
    void 없는_자리로는_못_옮긴다() {
        given(zoneRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeService.updateZone(EXCHANGE_ID, ME, 404L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.ZONE_NOT_FOUND);
    }

    @Test
    @DisplayName("아직 수락하지 않아 자리가 없는 교환은 옮길 수 없다")
    void 자리가_없는_교환은_못_옮긴다() throws Exception {
        Exchange pending = withId(Exchange.create(ExchangeType.ONE_TO_ONE), 77L);
        given(exchangeRepository.findById(77L)).willReturn(Optional.of(pending));
        given(participantRepository.findAllByExchangeId(77L))
                .willReturn(List.of(ExchangeParticipant.accepted(pending, me)));

        assertThatThrownBy(() -> exchangeService.updateZone(77L, ME, 2L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.EXCHANGE_NOT_ACCEPTED);
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
    @DisplayName("한 명이 만났다고 하면 다른 한 명은 취소할 수 없다")
    void 먼저_누른_한_번만_반영된다() {
        exchange.confirmTime(1);

        exchangeService.complete(EXCHANGE_ID, ME);

        assertThatThrownBy(() -> exchangeService.cancel(EXCHANGE_ID, PARTNER))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.EXCHANGE_ALREADY_FINISHED);
    }

    @Test
    @DisplayName("한 명이 취소하면 다른 한 명은 만났다고 할 수 없다")
    void 취소된_약속은_끝낼_수_없다() {
        exchangeService.cancel(EXCHANGE_ID, ME);

        assertThatThrownBy(() -> exchangeService.complete(EXCHANGE_ID, PARTNER))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.EXCHANGE_ALREADY_FINISHED);
    }

    @Test
    @DisplayName("교환을 마치면 참가자 전원에게 알린다")
    void 교환을_마치면_전원에게_알린다() {
        exchange.confirmTime(1);

        exchangeService.complete(EXCHANGE_ID, ME);

        verify(sseEventPublisher).toUser(eq(PARTNER), eq(SseEventType.EXCHANGE_COMPLETED), any());
    }

    @Test
    @DisplayName("교환을 마치면 준 사람의 보유 수량이 준다")
    void 교환을_마치면_준_사람의_보유_수량이_준다() throws Exception {
        Item item = withId(Item.of(null, "카드", null), ITEM_ID);
        UserHaveItem meHave = UserHaveItem.of(me, item, 3);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, me, item, partner, 1)));
        given(userHaveItemRepository.findByUserIdAndItemId(ME, ITEM_ID)).willReturn(Optional.of(meHave));

        exchangeService.complete(EXCHANGE_ID, ME);

        assertThat(meHave.getQuantityLeft()).isEqualTo(2);
        assertThat(meHave.getStatus()).isEqualTo(ItemStatus.LEFT);
    }

    @Test
    @DisplayName("받는 사람에게 같은 카드 행이 없으면 OUT 상태로 새로 만든다")
    void 받는_사람에게_같은_카드_행이_없으면_OUT_상태로_새로_만든다() throws Exception {
        Item item = withId(Item.of(null, "카드", null), ITEM_ID);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, me, item, partner, 2)));
        given(userHaveItemRepository.findByUserIdAndItemId(PARTNER, ITEM_ID)).willReturn(Optional.empty());

        exchangeService.complete(EXCHANGE_ID, ME);

        verify(userHaveItemRepository).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("받는 사람에게 이미 행이 있으면 수량만 더하고 상태는 그대로 둔다")
    void 받는_사람에게_이미_행이_있으면_수량만_더한다() throws Exception {
        Item item = withId(Item.of(null, "카드", null), ITEM_ID);
        UserHaveItem partnerHave = UserHaveItem.of(partner, item, 2);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, me, item, partner, 1)));
        given(userHaveItemRepository.findByUserIdAndItemId(PARTNER, ITEM_ID)).willReturn(Optional.of(partnerHave));

        exchangeService.complete(EXCHANGE_ID, ME);

        assertThat(partnerHave.getQuantity()).isEqualTo(3);
        assertThat(partnerHave.getQuantityLeft()).isEqualTo(2);
        assertThat(partnerHave.getStatus()).isEqualTo(ItemStatus.LEFT);
        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("찾던 카드였으면 찾는 수량이 준다")
    void 찾던_카드였으면_찾는_수량이_준다() throws Exception {
        Item item = withId(Item.of(null, "카드", null), ITEM_ID);
        UserWantItem partnerWant = UserWantItem.of(partner, item, 3);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, me, item, partner, 1)));
        given(userWantItemRepository.findByUserIdAndItemId(PARTNER, ITEM_ID)).willReturn(Optional.of(partnerWant));

        exchangeService.complete(EXCHANGE_ID, ME);

        assertThat(partnerWant.getQuantity()).isEqualTo(2);
        verify(userWantItemRepository, never()).delete(any(UserWantItem.class));
    }

    @Test
    @DisplayName("찾던 카드가 다 채워지면 행을 지운다")
    void 찾던_카드가_다_채워지면_행을_지운다() throws Exception {
        Item item = withId(Item.of(null, "카드", null), ITEM_ID);
        UserWantItem partnerWant = UserWantItem.of(partner, item, 1);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, me, item, partner, 1)));
        given(userWantItemRepository.findByUserIdAndItemId(PARTNER, ITEM_ID)).willReturn(Optional.of(partnerWant));

        exchangeService.complete(EXCHANGE_ID, ME);

        verify(userWantItemRepository).delete(partnerWant);
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

    /** 만든 교환을 화면이 받는 모양으로 읽는다. 조회 경로와 같은 값을 보게 된다. */
    private ExchangeResponseDto toResponseOf(Exchange created) {
        return exchangeService.find(created.getId());
    }

    // ──────────────────────────────────────────
    // 카드 수명주기 — 예약 해제와 소진
    // ──────────────────────────────────────────

    /**
     * 약속을 취소하면 잡아 뒀던 카드가 풀려야 한다.
     *
     * <p>풀리지 않으면 카드가 {@code RESERVED} 에 갇힌다. 매칭 쿼리가 {@code LEFT} 인 카드만 보기
     * 때문에, 그 사람은 취소 한 번으로 다시는 매칭되지 않는다.
     */
    @Test
    @DisplayName("약속을 취소하면 잡아 둔 카드가 풀리고 재매칭이 걸린다")
    void 취소하면_카드가_풀린다() throws Exception {
        UserHaveItem reserved = reservedHaveItem();
        givenExchangeItem(reserved);

        exchangeService.cancel(EXCHANGE_ID, ME);

        assertThat(reserved.isReserved()).isFalse();
        // 카드가 풀렸으니 남은 사람들은 다시 상대를 찾아야 한다.
        verify(eventPublisher, times(2)).publishEvent(any(MatchTriggerEvent.class));
    }

    @Test
    @DisplayName("교환을 마치면 내놓은 수량이 줄고 찾는 카드가 빠진다")
    void 마치면_수량이_줄어든다() throws Exception {
        exchange.confirmTime(1);

        UserHaveItem given = reservedHaveItem();
        UserWantItem wanted = UserWantItem.of(partner, given.getItem(), 1);
        givenExchangeItem(given);
        given(userWantItemRepository.findByUserIdAndItemId(PARTNER, given.getItem().getId()))
                .willReturn(Optional.of(wanted));

        exchangeService.complete(EXCHANGE_ID, ME);

        // 두 장 중 한 장이 나갔다.
        assertThat(given.getQuantityLeft()).isEqualTo(1);
        // 받은 사람은 더 이상 이 카드를 찾지 않는다. 남겨 두면 곧바로 같은 카드로 재매칭된다.
        assertThat(wanted.getQuantity()).isZero();
        verify(userWantItemRepository).delete(wanted);
    }

    /** ME 가 PARTNER 에게 카드 한 장을 주는 교환 한 줄을 깔아 둔다. */
    private void givenExchangeItem(UserHaveItem haveItem) {
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.of(exchange, me, haveItem.getItem(), partner)));
        given(userHaveItemRepository.findByUserIdAndItemId(ME, haveItem.getItem().getId()))
                .willReturn(Optional.of(haveItem));
    }

    /** 매칭이 잡아 둔 상태의 보유 카드. 두 장 중 한 장이 이번 교환에 걸려 있다. */
    private UserHaveItem reservedHaveItem() throws Exception {
        Item item = withId(Item.of(exchange.getZone().getBooth(), "IONIQ 5 N", null), 7L);
        UserHaveItem haveItem = UserHaveItem.of(me, item, 2);
        haveItem.reserve();
        return haveItem;
    }
}
