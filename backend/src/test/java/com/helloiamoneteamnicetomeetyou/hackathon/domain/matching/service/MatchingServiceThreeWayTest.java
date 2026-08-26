package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeLock;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
 * 3인 교환 사이클 탐색.
 *
 * <p>A → B → C → A 로 도는 조합을 찾는 자리다. 조합이 여럿일 때 실제로 성립하는 것을 골라내는지,
 * 고른 카드가 그새 사라졌을 때 터지지 않는지를 본다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("3인 교환 사이클 탐색")
class MatchingServiceThreeWayTest {

    private static final Long BOOTH_ID = 1L;
    private static final Long EXCHANGE_ID = 7L;

    private static final UUID A = UUID.fromString("aaaaaaaa-1111-4111-8111-111111111111");
    private static final UUID B = UUID.fromString("bbbbbbbb-2222-4222-8222-222222222222");
    /** 나에게 줄 수 있는 카드가 내가 B 에게 주는 카드뿐인 후보. 사이클이 성립하지 않는다. */
    private static final UUID C_DEAD = UUID.fromString("cccccccc-3333-4333-8333-333333333333");
    /** 나에게 다른 카드를 줄 수 있는 후보. 이쪽으로는 사이클이 성립한다. */
    private static final UUID C_LIVE = UUID.fromString("dddddddd-4444-4444-8444-444444444444");

    /** A 가 내놓은 카드. B 가 이걸 원한다. */
    private static final Long A_CARD = 10L;
    /** A 가 찾는 카드. C_LIVE 가 이걸 들고 있다. */
    private static final Long WANTED_CARD = 11L;
    /** B 가 C_DEAD 에게 줄 수 있는 카드. */
    private static final Long B_TO_DEAD_CARD = 20L;
    /** B 가 C_LIVE 에게 줄 수 있는 카드. */
    private static final Long B_TO_LIVE_CARD = 21L;

    @Mock
    private UserHaveItemRepository userHaveItemRepository;
    @Mock
    private UserWantItemRepository userWantItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ExchangeRepository exchangeRepository;
    @Mock
    private ExchangeParticipantRepository exchangeParticipantRepository;
    @Mock
    private ExchangeItemRepository exchangeItemRepository;
    @Mock
    private SseEventPublisher sseEventPublisher;
    @Mock
    private ExchangeLock exchangeLock;

    @InjectMocks
    private MatchingService matchingService;

    private User userA;
    private User userB;
    private User userCLive;
    private Item aCard;
    private Item wantedCard;
    private Item bToLiveCard;

    @BeforeEach
    void setUp() throws Exception {
        Booth booth = withId(Booth.of("현대자동차 팝업", null), BOOTH_ID);

        userA = User.of(A, "레몬 28");
        userB = User.of(B, "사과 31");
        userCLive = User.of(C_LIVE, "체리 12");

        aCard = withId(Item.of(booth, "아이오닉 5", "A 가 내놓은 카드"), A_CARD);
        wantedCard = withId(Item.of(booth, "아이오닉 6", "A 가 찾는 카드"), WANTED_CARD);
        bToLiveCard = withId(Item.of(booth, "코나", "B 가 넘길 카드"), B_TO_LIVE_CARD);

        given(userRepository.findById(A)).willReturn(Optional.of(userA));
        given(userRepository.findById(B)).willReturn(Optional.of(userB));
        given(userRepository.findById(C_LIVE)).willReturn(Optional.of(userCLive));

        given(exchangeParticipantRepository.existsActiveExchange(A)).willReturn(false);
        given(exchangeLock.acquire(any())).willReturn(true);

        // 쿼리 A: 내 카드를 원하는 사람은 B 뿐이다.
        given(userWantItemRepository.findToThemData(A.toString()))
                .willReturn(List.<Object[]>of(row(B.toString(), A_CARD, 1, 100L)));

        /*
          쿼리 B: 내가 찾는 카드를 가진 사람들.

          C_DEAD 가 들고 있는 것은 A 가 내놓은 카드와 같은 카드다. 예전에 보유와 희망 양쪽에
          등록해 둔 행이 남아 있으면 이렇게 잡힌다. 이걸로 사이클을 닫으면 A 가 내놓은 카드를
          한 바퀴 돌아 도로 받게 되어 교환이 아니다.
        */
        given(userHaveItemRepository.findToMeData(A.toString()))
                .willReturn(List.<Object[]>of(
                        row(C_DEAD.toString(), A_CARD, 1),
                        row(C_LIVE.toString(), WANTED_CARD, 1)));

        given(itemRepository.findBoothIdsByItemIds(anyCollection()))
                .willReturn(List.<Object[]>of(
                        row(A_CARD, BOOTH_ID),
                        row(WANTED_CARD, BOOTH_ID)));

        // 쿼리 C: B 는 두 후보 모두에게 카드를 넘길 수 있다. C_DEAD 가 먼저 나온다.
        given(userHaveItemRepository.findBToCData(eq(BOOTH_ID), anyCollection(), anyCollection()))
                .willReturn(List.<Object[]>of(
                        row(B.toString(), C_DEAD.toString(), B_TO_DEAD_CARD, 1),
                        row(B.toString(), C_LIVE.toString(), B_TO_LIVE_CARD, 1)));

        given(exchangeRepository.save(any(Exchange.class)))
                .willAnswer(call -> withId(call.getArgument(0), EXCHANGE_ID));
    }

    @Test
    @DisplayName("첫 조합이 한 바퀴 돌아 제 카드를 받는 꼴이면 다음 조합으로 사이클을 닫는다")
    void 사이클이_안_되는_조합은_건너뛴다() {
        given(userHaveItemRepository.findByUserIdAndItemIds(A, Set.of(A_CARD)))
                .willReturn(List.of(UserHaveItem.of(userA, aCard, 1)));
        given(userHaveItemRepository.findByUserIdAndItemIds(B, Set.of(B_TO_LIVE_CARD)))
                .willReturn(List.of(UserHaveItem.of(userB, bToLiveCard, 1)));
        given(userHaveItemRepository.findByUserIdAndItemIds(C_LIVE, Set.of(WANTED_CARD)))
                .willReturn(List.of(UserHaveItem.of(userCLive, wantedCard, 1)));

        matchingService.runMatching(A);

        ArgumentCaptor<Exchange> saved = ArgumentCaptor.forClass(Exchange.class);
        verify(exchangeRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(ExchangeType.MULTI_WAY);

        // 세 다리가 A → B → C_LIVE → A 로 이어져야 한다. C_DEAD 는 끼지 않는다.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExchangeItem>> items = ArgumentCaptor.forClass(List.class);
        verify(exchangeItemRepository).saveAll(items.capture());

        assertThat(items.getValue())
                .extracting(
                        item -> item.getFromUser().getId(),
                        item -> item.getItem().getId(),
                        item -> item.getToUser().getId())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(A, A_CARD, B),
                        org.assertj.core.groups.Tuple.tuple(B, B_TO_LIVE_CARD, C_LIVE),
                        org.assertj.core.groups.Tuple.tuple(C_LIVE, WANTED_CARD, A));
    }

    @Test
    @DisplayName("고른 카드가 그새 다른 교환에 묶였으면 터지지 않고 교환을 만들지 않는다")
    void 카드가_사라지면_만들지_않는다() {
        given(userHaveItemRepository.findByUserIdAndItemIds(A, Set.of(A_CARD)))
                .willReturn(List.of(UserHaveItem.of(userA, aCard, 1)));
        // 후보를 고른 뒤 B 가 이 카드를 다른 교환에 전부 내줬다. quantityLeft 조건에 걸려 안 나온다.
        given(userHaveItemRepository.findByUserIdAndItemIds(B, Set.of(B_TO_LIVE_CARD)))
                .willReturn(List.of());
        given(userHaveItemRepository.findByUserIdAndItemIds(C_LIVE, Set.of(WANTED_CARD)))
                .willReturn(List.of(UserHaveItem.of(userCLive, wantedCard, 1)));

        matchingService.runMatching(A);

        verify(exchangeRepository, never()).save(any(Exchange.class));
        verify(exchangeItemRepository, never()).saveAll(any());
    }

    // ──────────────────────────────────────────

    /** 네이티브 쿼리가 돌려주는 {@code Object[]} 한 줄. */
    private static Object[] row(Object... values) {
        return values;
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        return entity;
    }
}
