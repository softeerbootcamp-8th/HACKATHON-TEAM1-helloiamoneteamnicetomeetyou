package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto.MatchSuggestedResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
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

/**
 * 대기 중인 매칭 제안 조회.
 *
 * <p>실시간 연결이 끊겼던 동안 온 {@code MATCH_SUGGESTED} 는 다시 오지 않는다. 재연결한 화면이
 * 이걸로 자기에게 온 제안을 다시 읽는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("대기 중인 매칭 제안 조회")
class MatchingServicePendingTest {

    private static final Long EXCHANGE_ID = 1L;
    private static final UUID ME = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PARTNER = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 8, 26, 14, 15);

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

    @InjectMocks
    private MatchingService matchingService;

    private Booth booth;
    private User me;
    private User partner;
    private Item myCard;
    private Item theirCard;

    @BeforeEach
    void setUp() throws Exception {
        booth = withId(Booth.of("현대자동차 팝업", null), 1L);
        me = User.of(ME, "레몬 28");
        partner = User.of(PARTNER, "블루N");
        myCard = withId(Item.of(booth, "아이오닉 5", "내가 가진 카드"), 10L);
        theirCard = withId(Item.of(booth, "아이오닉 6", "내가 원하는 카드"), 11L);

        given(userRepository.findById(ME)).willReturn(Optional.of(me));
    }

    @Test
    @DisplayName("아직 수락하지 않은 제안을 내 관점으로 돌려준다")
    void 대기_중인_제안을_돌려준다() throws Exception {
        Exchange exchange = pendingExchange();
        given(exchangeRepository.findActiveByUserId(ME, List.of(ExchangeStatus.PENDING)))
                .willReturn(List.of(exchange));
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID)).willReturn(itemsOf(exchange));

        MatchSuggestedResponseDto suggestion = matchingService.findPendingSuggestionOf(ME);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.exchangeId()).isEqualTo(EXCHANGE_ID);
        // 내가 주는 것과 받는 것이 내 관점으로 갈려 있어야 한다. SSE 로 오는 payload 와 같은 모양이다.
        assertThat(suggestion.giveItems()).extracting("name").containsExactly("아이오닉 5");
        assertThat(suggestion.giveTo().id()).isEqualTo(PARTNER);
        assertThat(suggestion.receiveItems()).extracting("name").containsExactly("아이오닉 6");
        assertThat(suggestion.receiveFrom().id()).isEqualTo(PARTNER);
    }

    @Test
    @DisplayName("이미 수락해서 약속이 잡힌 교환은 제안으로 보지 않는다")
    void 약속이_잡힌_교환은_제외한다() throws Exception {
        Exchange exchange = pendingExchange();
        // 참가자가 결과를 보고 장소를 잡으러 들어가면 자리와 격자가 붙는다. 그때부터는
        // 매칭 제안이 아니라 약속이고, GET /api/exchanges/active 가 다룬다.
        Zone zone = withId(Zone.of(booth, "중앙 포토존 앞", "행사 중앙 포토존"), 1L);
        exchange.prepareAppointment(zone, BASE_TIME, 2, 28);

        given(exchangeRepository.findActiveByUserId(ME, List.of(ExchangeStatus.PENDING)))
                .willReturn(List.of(exchange));

        assertThat(matchingService.findPendingSuggestionOf(ME)).isNull();
        // 걸러진 교환의 아이템은 읽지 않는다.
        verify(exchangeItemRepository, never()).findByExchangeId(EXCHANGE_ID);
    }

    @Test
    @DisplayName("대기 중인 제안이 없으면 null 이다")
    void 제안이_없으면_null() {
        given(exchangeRepository.findActiveByUserId(ME, List.of(ExchangeStatus.PENDING)))
                .willReturn(List.of());

        assertThat(matchingService.findPendingSuggestionOf(ME)).isNull();
    }

    @Test
    @DisplayName("주고받을 것이 한쪽이라도 없는 교환은 터지지 않고 건너뛴다")
    void 주고받을_것이_없으면_건너뛴다() throws Exception {
        Exchange exchange = pendingExchange();
        given(exchangeRepository.findActiveByUserId(ME, List.of(ExchangeStatus.PENDING)))
                .willReturn(List.of(exchange));
        // 내가 받을 것만 있고 줄 것이 없다. 제안으로 성립하지 않는 데이터다.
        given(exchangeItemRepository.findByExchangeId(EXCHANGE_ID))
                .willReturn(List.of(ExchangeItem.of(exchange, partner, theirCard, me)));

        assertThat(matchingService.findPendingSuggestionOf(ME)).isNull();
    }

    @Test
    @DisplayName("없는 사용자로 물으면 예외다")
    void 없는_사용자는_예외() {
        given(userRepository.findById(ME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.findPendingSuggestionOf(ME))
                .isInstanceOf(ApplicationException.class);
    }

    // ──────────────────────────────────────────

    /** 매칭이 방금 만든 교환. 자리도 시간도 아직 없다. */
    private Exchange pendingExchange() throws Exception {
        return withId(Exchange.create(ExchangeType.ONE_TO_ONE), EXCHANGE_ID);
    }

    /** 1:1 교환이라 내가 주는 것과 받는 것이 한 장씩이다. */
    private List<ExchangeItem> itemsOf(Exchange exchange) {
        return List.of(
                ExchangeItem.of(exchange, me, myCard, partner),
                ExchangeItem.of(exchange, partner, theirCard, me));
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        return entity;
    }
}
