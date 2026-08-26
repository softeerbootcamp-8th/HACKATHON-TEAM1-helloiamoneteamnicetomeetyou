package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.ItemStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("교환 완료")
class ExchangeServiceTest {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long ITEM_ID = 10L;

    private final User giver = User.of(UUID.randomUUID());
    private final User receiver = User.of(UUID.randomUUID());
    private final Item item = mockItem();

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ExchangeParticipantRepository exchangeParticipantRepository;

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

    private Exchange inProgressExchange() {
        Exchange exchange = Exchange.create(ExchangeType.ONE_TO_ONE);
        exchange.startProgress();
        return exchange;
    }

    private void participantsAre(Exchange exchange, User... users) {
        List<ExchangeParticipant> participants = List.of(users).stream()
                .map(user -> ExchangeParticipant.create(exchange, user))
                .toList();
        given(exchangeParticipantRepository.findAllByExchangeId(EXCHANGE_ID)).willReturn(participants);
    }

    private static Item mockItem() {
        Item item = Mockito.mock(Item.class);
        given(item.getId()).willReturn(ITEM_ID);
        return item;
    }

    @Test
    @DisplayName("완료하면 상태가 COMPLETED 로 바뀐다")
    void 완료하면_상태가_COMPLETED_로_바뀐다() {
        Exchange exchange = inProgressExchange();
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID)).willReturn(List.of());

        exchangeService.complete(EXCHANGE_ID, giver.getId());

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.COMPLETED);
    }

    @Test
    @DisplayName("준 사람의 보유 수량이 준다")
    void 준_사람의_보유_수량이_준다() {
        Exchange exchange = inProgressExchange();
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);

        UserHaveItem giverHave = UserHaveItem.of(giver, item, 3);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, giver, item, receiver, 1)));
        given(userHaveItemRepository.findByUserIdAndItemId(giver.getId(), ITEM_ID))
                .willReturn(Optional.of(giverHave));
        given(userHaveItemRepository.findByUserIdAndItemId(receiver.getId(), ITEM_ID))
                .willReturn(Optional.empty());

        exchangeService.complete(EXCHANGE_ID, giver.getId());

        assertThat(giverHave.getQuantityLeft()).isEqualTo(2);
        assertThat(giverHave.getStatus()).isEqualTo(ItemStatus.LEFT);
    }

    @Test
    @DisplayName("받는 사람에게 같은 카드 행이 없으면 OUT 상태로 새로 만든다")
    void 받는_사람에게_같은_카드_행이_없으면_OUT_상태로_새로_만든다() {
        Exchange exchange = inProgressExchange();
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);

        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, giver, item, receiver, 2)));
        given(userHaveItemRepository.findByUserIdAndItemId(giver.getId(), ITEM_ID))
                .willReturn(Optional.of(UserHaveItem.of(giver, item, 5)));
        given(userHaveItemRepository.findByUserIdAndItemId(receiver.getId(), ITEM_ID))
                .willReturn(Optional.empty());

        exchangeService.complete(EXCHANGE_ID, giver.getId());

        ArgumentCaptor<UserHaveItem> captor = ArgumentCaptor.forClass(UserHaveItem.class);
        verify(userHaveItemRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(receiver);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
        assertThat(captor.getValue().getQuantityLeft()).isEqualTo(0);
        assertThat(captor.getValue().getStatus()).isEqualTo(ItemStatus.OUT);
    }

    @Test
    @DisplayName("받는 사람에게 이미 행이 있으면 수량만 더하고 상태는 그대로 둔다")
    void 받는_사람에게_이미_행이_있으면_수량만_더한다() {
        Exchange exchange = inProgressExchange();
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);

        UserHaveItem receiverHave = UserHaveItem.of(receiver, item, 2);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, giver, item, receiver, 1)));
        given(userHaveItemRepository.findByUserIdAndItemId(giver.getId(), ITEM_ID))
                .willReturn(Optional.of(UserHaveItem.of(giver, item, 5)));
        given(userHaveItemRepository.findByUserIdAndItemId(receiver.getId(), ITEM_ID))
                .willReturn(Optional.of(receiverHave));

        exchangeService.complete(EXCHANGE_ID, giver.getId());

        assertThat(receiverHave.getQuantity()).isEqualTo(3);
        assertThat(receiverHave.getQuantityLeft()).isEqualTo(2);
        assertThat(receiverHave.getStatus()).isEqualTo(ItemStatus.LEFT);
        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("찾던 카드였으면 찾는 수량이 준다")
    void 찾던_카드였으면_찾는_수량이_준다() {
        Exchange exchange = inProgressExchange();
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);

        UserWantItem receiverWant = UserWantItem.of(receiver, item, 3);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, giver, item, receiver, 1)));
        given(userHaveItemRepository.findByUserIdAndItemId(giver.getId(), ITEM_ID))
                .willReturn(Optional.of(UserHaveItem.of(giver, item, 5)));
        given(userHaveItemRepository.findByUserIdAndItemId(receiver.getId(), ITEM_ID))
                .willReturn(Optional.empty());
        given(userWantItemRepository.findByUserIdAndItemId(receiver.getId(), ITEM_ID))
                .willReturn(Optional.of(receiverWant));

        exchangeService.complete(EXCHANGE_ID, giver.getId());

        assertThat(receiverWant.getQuantity()).isEqualTo(2);
        verify(userWantItemRepository, never()).delete(any(UserWantItem.class));
    }

    @Test
    @DisplayName("찾던 카드가 다 채워지면 행을 지운다")
    void 찾던_카드가_다_채워지면_행을_지운다() {
        Exchange exchange = inProgressExchange();
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);

        UserWantItem receiverWant = UserWantItem.of(receiver, item, 1);
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.create(exchange, giver, item, receiver, 1)));
        given(userHaveItemRepository.findByUserIdAndItemId(giver.getId(), ITEM_ID))
                .willReturn(Optional.of(UserHaveItem.of(giver, item, 5)));
        given(userHaveItemRepository.findByUserIdAndItemId(receiver.getId(), ITEM_ID))
                .willReturn(Optional.empty());
        given(userWantItemRepository.findByUserIdAndItemId(receiver.getId(), ITEM_ID))
                .willReturn(Optional.of(receiverWant));

        exchangeService.complete(EXCHANGE_ID, giver.getId());

        verify(userWantItemRepository).delete(receiverWant);
    }

    @Test
    @DisplayName("진행 중이 아니면 아무것도 하지 않는다")
    void 진행중이_아니면_아무것도_하지_않는다() {
        Exchange exchange = Exchange.create(ExchangeType.ONE_TO_ONE);
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);

        exchangeService.complete(EXCHANGE_ID, giver.getId());

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.PENDING);
        verify(exchangeItemRepository, never()).findByExchangeId(any());
    }

    @Test
    @DisplayName("참가자가 아니면 예외를 던진다")
    void 참가자가_아니면_예외를_던진다() {
        Exchange exchange = inProgressExchange();
        given(exchangeRepository.findById(EXCHANGE_ID)).willReturn(Optional.of(exchange));
        participantsAre(exchange, giver, receiver);

        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> exchangeService.complete(EXCHANGE_ID, stranger))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.NOT_EXCHANGE_PARTICIPANT);
    }
}
