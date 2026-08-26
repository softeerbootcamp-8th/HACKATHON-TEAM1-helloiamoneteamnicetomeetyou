package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeAnswerResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.ReceivedPokeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.SentPokeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.repository.PokeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorType;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 찔러보기.
 *
 * <p><b>엔티티를 mock 으로 만든다.</b> {@code id} 가 DB 시퀀스라 정적 팩토리로는 채울 수 없는데,
 * 고른 카드를 찾아내는 것이 {@code itemId} 비교라서 이 값이 없으면 검증이 성립하지 않는다.
 *
 * <p><b>픽스처는 전부 {@code given(...)} 밖에서 만든다.</b> {@code willReturn(user(...))} 처럼
 * 인자 안에서 스터빙하면 Mockito 가 {@code UnfinishedStubbingException} 을 던진다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("찔러보기")
class PokeServiceTest {

    private static final UUID SENDER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long BOOTH_ID = 1L;
    private static final UUID RECEIVER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Long REQUESTED_ITEM_ID = 10L;
    private static final Long OFFERED_ITEM_ID = 20L;
    private static final Long POKE_ID = 5L;

    @Mock
    private PokeRepository pokeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserHaveItemRepository userHaveItemRepository;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private ExchangeItemRepository exchangeItemRepository;

    @Mock
    private SseEventPublisher sseEventPublisher;

    @InjectMocks
    private PokeService pokeService;

    private User sender;
    private User receiver;
    private Item requestedItem;
    private Item offeredItem;
    private UserHaveItem receiverHasRequested;
    private UserHaveItem senderHasOffered;

    @BeforeEach
    void 픽스처를_미리_만든다() {
        sender = user(SENDER);
        receiver = user(RECEIVER);
        requestedItem = item(REQUESTED_ITEM_ID);
        offeredItem = item(OFFERED_ITEM_ID);
        receiverHasRequested = have(receiver, requestedItem, 1);
        senderHasOffered = have(sender, offeredItem, 3);
    }

    // ---------- 보내기 ----------

    @Test
    @DisplayName("상대가 그 카드를 내놓고 있고 내가 내놓을 카드가 있으면 저장하고 알린다")
    void 정상적으로_보낸다() {
        Poke saved = Poke.of(sender, receiver, requestedItem);
        보낼_수_있는_상태();
        given(pokeRepository.save(any(Poke.class))).willReturn(saved);

        pokeService.send(SENDER, RECEIVER, REQUESTED_ITEM_ID);

        ArgumentCaptor<Poke> captor = ArgumentCaptor.forClass(Poke.class);
        verify(pokeRepository).save(captor.capture());
        assertThat(captor.getValue().getFromUser()).isEqualTo(sender);
        assertThat(captor.getValue().getToUser()).isEqualTo(receiver);
        assertThat(captor.getValue().getRequestedItem()).isEqualTo(requestedItem);
        assertThat(captor.getValue().getStatus()).isEqualTo(PokeStatus.PENDING);
        // 내줄 카드는 받는 쪽이 고른다. 보낼 때는 비어 있어야 한다.
        assertThat(captor.getValue().getChosenItem()).isNull();

        verify(sseEventPublisher).toUser(eq(RECEIVER), eq(SseEventType.POKE_RECEIVED), any());
    }

    @Test
    @DisplayName("같은 상대에게 답을 기다리는 중이면 재신청을 막는다")
    void 같은_상대에게_대기중이면_막는다() {
        보낼_수_있는_상태();
        given(pokeRepository.existsByFromUserIdAndToUserIdAndStatus(
                SENDER, RECEIVER, PokeStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> pokeService.send(SENDER, RECEIVER, REQUESTED_ITEM_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_DUPLICATE_PENDING);

        verify(pokeRepository, never()).save(any(Poke.class));
        verify(sseEventPublisher, never()).toUser(any(), any(), any());
    }

    @Test
    @DisplayName("자신에게는 찔러볼 수 없다")
    void 자신에게는_못_보낸다() {
        assertThatThrownBy(() -> pokeService.send(SENDER, SENDER, REQUESTED_ITEM_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_SELF_NOT_ALLOWED);
    }

    @Test
    @DisplayName("상대가 가진 카드가 아니면 막는다")
    void 상대가_안_가진_카드면_막는다() {
        사용자는_둘_다_있다();
        given(userHaveItemRepository.findByUserIdAndItemId(RECEIVER, REQUESTED_ITEM_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> pokeService.send(SENDER, RECEIVER, REQUESTED_ITEM_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_ITEM_NOT_OWNED);
    }

    @Test
    @DisplayName("상대의 그 카드가 다 나갔으면 막는다")
    void 수량이_0이면_막는다() {
        UserHaveItem soldOut = have(receiver, requestedItem, 0);
        사용자는_둘_다_있다();
        given(userHaveItemRepository.findByUserIdAndItemId(RECEIVER, REQUESTED_ITEM_ID))
                .willReturn(Optional.of(soldOut));

        assertThatThrownBy(() -> pokeService.send(SENDER, RECEIVER, REQUESTED_ITEM_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_ITEM_SOLD_OUT);
    }

    @Test
    @DisplayName("내놓을 카드가 하나도 없으면 막는다")
    void 내놓을_카드가_없으면_막는다() {
        사용자는_둘_다_있다();
        given(userHaveItemRepository.findByUserIdAndItemId(RECEIVER, REQUESTED_ITEM_ID))
                .willReturn(Optional.of(receiverHasRequested));
        given(userHaveItemRepository.findAllByUserId(SENDER)).willReturn(List.of());

        assertThatThrownBy(() -> pokeService.send(SENDER, RECEIVER, REQUESTED_ITEM_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_NO_OFFERABLE_ITEM);
    }

    @Test
    @DisplayName("등록되지 않은 사용자면 USER_NOT_FOUND 다")
    void 등록되지_않은_사용자면_막는다() {
        given(userRepository.findById(SENDER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pokeService.send(SENDER, RECEIVER, REQUESTED_ITEM_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // ---------- 응답 ----------

    @Test
    @DisplayName("수락하면 교환 카드 두 줄을 만들고 방향이 서로 반대다")
    void 수락하면_교환을_만든다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        Exchange exchange = 성사된_교환();
        수락할_수_있는_상태(poke, exchange);

        PokeAnswerResponseDto response =
                pokeService.answer(POKE_ID, RECEIVER, PokeStatus.ACCEPTED, OFFERED_ITEM_ID);

        assertThat(response.status()).isEqualTo(PokeStatus.ACCEPTED);
        assertThat(response.giveItemId()).isEqualTo(REQUESTED_ITEM_ID);
        assertThat(response.receiveItemId()).isEqualTo(OFFERED_ITEM_ID);
        assertThat(poke.getChosenItem()).isEqualTo(offeredItem);
        assertThat(poke.getExchange()).isEqualTo(exchange);
        assertThat(poke.getRespondedAt()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExchangeItem>> items = ArgumentCaptor.forClass(List.class);
        verify(exchangeItemRepository).saveAll(items.capture());
        assertThat(items.getValue()).hasSize(2);

        // 보낸 사람은 고른 카드를 내주고, 받은 사람은 요청받은 카드를 내준다.
        ExchangeItem fromSender = items.getValue().get(0);
        assertThat(fromSender.getFromUser()).isEqualTo(sender);
        assertThat(fromSender.getItem()).isEqualTo(offeredItem);
        assertThat(fromSender.getToUser()).isEqualTo(receiver);

        ExchangeItem fromReceiver = items.getValue().get(1);
        assertThat(fromReceiver.getFromUser()).isEqualTo(receiver);
        assertThat(fromReceiver.getItem()).isEqualTo(requestedItem);
        assertThat(fromReceiver.getToUser()).isEqualTo(sender);

        verify(sseEventPublisher).toUser(eq(SENDER), eq(SseEventType.POKE_ACCEPTED), any());
    }

    @Test
    @DisplayName("수락하면 두 사람으로 교환을 만들어 달라고 맡긴다")
    void 수락하면_교환_생성을_맡긴다() {
        수락할_수_있는_상태(Poke.of(sender, receiver, requestedItem), 성사된_교환());

        pokeService.answer(POKE_ID, RECEIVER, PokeStatus.ACCEPTED, OFFERED_ITEM_ID);

        // 만나는 자리와 시간 격자, 약속 식별자가 거기서 함께 붙는다. 여기서 직접 만들지 않는다.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeService)
                .createExchange(eq(BOOTH_ID), eq(ExchangeType.ONE_TO_ONE), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(SENDER, RECEIVER);
    }

    @Test
    @DisplayName("거절하면 교환을 만들지 않고 보낸 사람에게 알린다")
    void 거절하면_교환을_만들지_않는다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));

        PokeAnswerResponseDto response =
                pokeService.answer(POKE_ID, RECEIVER, PokeStatus.REJECTED, null);

        assertThat(response.status()).isEqualTo(PokeStatus.REJECTED);
        assertThat(response.exchangeId()).isNull();
        assertThat(poke.getStatus()).isEqualTo(PokeStatus.REJECTED);
        verify(exchangeService, never()).createExchange(any(), any(), any());
        verify(exchangeItemRepository, never()).saveAll(anyList());
        verify(sseEventPublisher).toUser(eq(SENDER), eq(SseEventType.POKE_REJECTED), any());
    }

    @Test
    @DisplayName("받은 사람이 아니면 응답할 수 없다")
    void 받은_사람이_아니면_막는다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));

        assertThatThrownBy(() -> pokeService.answer(POKE_ID, SENDER, PokeStatus.REJECTED, null))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_NOT_RECEIVER);
    }

    @Test
    @DisplayName("이미 응답한 찔러보기면 막는다")
    void 이미_응답했으면_막는다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        poke.reject();
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));

        assertThatThrownBy(() -> pokeService.answer(POKE_ID, RECEIVER, PokeStatus.REJECTED, null))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_ALREADY_ANSWERED);
    }

    @Test
    @DisplayName("상대 묶음에 없는 카드를 고르면 막는다")
    void 묶음에_없는_카드를_고르면_막는다() {
        수락할_수_있는_상태(Poke.of(sender, receiver, requestedItem), 성사된_교환());

        assertThatThrownBy(() -> pokeService.answer(POKE_ID, RECEIVER, PokeStatus.ACCEPTED, 999L))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_CHOSEN_ITEM_NOT_OFFERED);

        verify(exchangeService, never()).createExchange(any(), any(), any());
    }

    @Test
    @DisplayName("수락하는데 고른 카드가 없으면 막는다")
    void 고른_카드가_없으면_막는다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));

        assertThatThrownBy(() -> pokeService.answer(POKE_ID, RECEIVER, PokeStatus.ACCEPTED, null))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("제안 뒤 요청받은 카드가 다 나갔으면 수락할 수 없다")
    void 요청받은_카드가_다_나갔으면_막는다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        UserHaveItem soldOut = have(receiver, requestedItem, 0);

        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));
        given(userHaveItemRepository.findAllByUserId(SENDER)).willReturn(List.of(senderHasOffered));
        given(userHaveItemRepository.findByUserIdAndItemId(RECEIVER, REQUESTED_ITEM_ID))
                .willReturn(Optional.of(soldOut));

        assertThatThrownBy(() ->
                pokeService.answer(POKE_ID, RECEIVER, PokeStatus.ACCEPTED, OFFERED_ITEM_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_ITEM_SOLD_OUT);

        verify(exchangeService, never()).createExchange(any(), any(), any());
    }

    @Test
    @DisplayName("없는 찔러보기면 POKE_NOT_FOUND 다")
    void 없는_찔러보기면_막는다() {
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pokeService.answer(POKE_ID, RECEIVER, PokeStatus.REJECTED, null))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.POKE_NOT_FOUND);
    }

    @Test
    @DisplayName("PENDING 으로는 응답할 수 없다")
    void PENDING_으로는_응답할_수_없다() {
        assertThatThrownBy(() -> pokeService.answer(POKE_ID, RECEIVER, PokeStatus.PENDING, null))
                .isInstanceOf(ApplicationException.class)
                .extracting(PokeServiceTest::errorTypeOf)
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    // ---------- 조회 ----------

    @Test
    @DisplayName("받은 목록의 묶음은 보낸 사람의 현재 보유 카드다")
    void 받은_목록은_현재_보유_카드를_준다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        // 수량이 0 인 카드는 묶음에서 빠져야 한다.
        UserHaveItem soldOut = have(sender, item(30L), 0);

        given(pokeRepository.findAllByToUserIdAndStatus(RECEIVER, PokeStatus.PENDING))
                .willReturn(List.of(poke));
        given(userHaveItemRepository.findAllByUserId(SENDER))
                .willReturn(List.of(senderHasOffered, soldOut));

        PageResponse<ReceivedPokeResponseDto> result = pokeService.findReceived(RECEIVER, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).offeredItems()).hasSize(1);
        assertThat(result.content().get(0).offeredItems().get(0).id()).isEqualTo(OFFERED_ITEM_ID);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("보낸 목록은 상대가 고른 카드를 함께 준다")
    void 보낸_목록은_고른_카드를_준다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        poke.accept(offeredItem, 성사된_교환());
        given(pokeRepository.findAllByFromUserId(SENDER)).willReturn(List.of(poke));

        PageResponse<SentPokeResponseDto> result = pokeService.findSent(SENDER, 0, 20);

        assertThat(result.content().get(0).status()).isEqualTo(PokeStatus.ACCEPTED);
        assertThat(result.content().get(0).chosenItem().id()).isEqualTo(OFFERED_ITEM_ID);
    }

    @Test
    @DisplayName("아직 답을 기다리는 건은 고른 카드가 응답에서 빠진다")
    void 대기중이면_고른_카드가_없다() {
        Poke poke = Poke.of(sender, receiver, requestedItem);
        given(pokeRepository.findAllByFromUserId(SENDER)).willReturn(List.of(poke));

        PageResponse<SentPokeResponseDto> result = pokeService.findSent(SENDER, 0, 20);

        assertThat(result.content().get(0).status()).isEqualTo(PokeStatus.PENDING);
        assertThat(result.content().get(0).chosenItem()).isNull();
        assertThat(result.content().get(0).exchangeId()).isNull();
    }

    // ---------- 전제 ----------

    private void 사용자는_둘_다_있다() {
        given(userRepository.findById(SENDER)).willReturn(Optional.of(sender));
        given(userRepository.findById(RECEIVER)).willReturn(Optional.of(receiver));
    }

    private void 보낼_수_있는_상태() {
        사용자는_둘_다_있다();
        given(userHaveItemRepository.findByUserIdAndItemId(RECEIVER, REQUESTED_ITEM_ID))
                .willReturn(Optional.of(receiverHasRequested));
        given(userHaveItemRepository.findAllByUserId(SENDER)).willReturn(List.of(senderHasOffered));
        given(pokeRepository.existsByFromUserIdAndToUserIdAndStatus(
                SENDER, RECEIVER, PokeStatus.PENDING)).willReturn(false);
    }

    private void 수락할_수_있는_상태(Poke poke, Exchange exchange) {
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));
        given(userHaveItemRepository.findAllByUserId(SENDER)).willReturn(List.of(senderHasOffered));
        given(userHaveItemRepository.findByUserIdAndItemId(RECEIVER, REQUESTED_ITEM_ID))
                .willReturn(Optional.of(receiverHasRequested));
        given(exchangeService.createExchange(any(), any(), any())).willReturn(exchange);
    }

    // ---------- 픽스처 ----------

    private static ErrorType errorTypeOf(Throwable e) {
        return ((ApplicationException) e).getErrorType();
    }

    private static User user(UUID id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private static Item item(long id) {
        Booth booth = mock(Booth.class);
        given(booth.getId()).willReturn(BOOTH_ID);

        Item item = mock(Item.class);
        given(item.getId()).willReturn(id);
        // 교환을 만들 때 이 부스의 만나는 자리를 고른다.
        given(item.getBooth()).willReturn(booth);
        return item;
    }

    private static UserHaveItem have(User user, Item item, int quantity) {
        UserHaveItem have = mock(UserHaveItem.class);
        given(have.getUser()).willReturn(user);
        given(have.getItem()).willReturn(item);
        given(have.getQuantity()).willReturn(quantity);
        given(have.getQuantityLeft()).willReturn(quantity);
        return have;
    }

    /**
     * 찔러보기가 성사되며 만들어지는 교환.
     *
     * <p>실제 흐름은 {@code ExchangeService.createExchange} 를 거쳐 만나는 자리와 시간 격자
     * 시작점, 약속별 식별자를 함께 받는다. 여기서는 교환 한 건이 있다는 것만 필요해서 그 값들을
     * 채우지 않는다.
     */
    private static Exchange 성사된_교환() {
        return Exchange.create(ExchangeType.ONE_TO_ONE);
    }
}
