package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.repository.NotificationRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.repository.PokeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.repository.PushSubscriptionRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("사람을 지울 때 걸려 있는 참조를 걷어낸다")
class AdminCleanupServiceTest {

    private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Mock
    private ExchangeRepository exchangeRepository;
    @Mock
    private ExchangeItemRepository exchangeItemRepository;
    @Mock
    private ExchangeParticipantRepository exchangeParticipantRepository;
    @Mock
    private ExchangeTimeSlotRepository exchangeTimeSlotRepository;
    @Mock
    private PokeRepository pokeRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock
    private UserHaveItemRepository userHaveItemRepository;
    @Mock
    private UserWantItemRepository userWantItemRepository;

    @InjectMocks
    private AdminCleanupService adminCleanupService;

    /**
     * {@code exchange_items} 의 from/to 는 비울 수 없는 자리라, 참가자 명단에 없는 교환이
     * 거기에만 남아 있으면 사람 삭제가 외래 키에 막힌다.
     */
    @Test
    @DisplayName("참가자 명단에 없고 오간 카드에만 남은 교환도 같이 지운다")
    void 명단과_오간_카드를_둘_다_본다() {
        given(exchangeParticipantRepository.findExchangeIdsByUserId(USER)).willReturn(List.of(1L, 2L));
        given(exchangeItemRepository.findExchangeIdsByUserId(USER)).willReturn(List.of(2L, 3L));

        int removed = adminCleanupService.deleteUserDeep(USER);

        ArgumentCaptor<List<Long>> deleted = ArgumentCaptor.captor();
        verify(exchangeRepository).deleteByIdIn(deleted.capture());
        // 2L 은 양쪽에 다 있어서 한 번만 지운다.
        assertThat(deleted.getValue()).containsExactly(1L, 2L, 3L);
        assertThat(removed).isEqualTo(3);
    }

    @Test
    @DisplayName("사람을 붙들고 있는 표를 하나도 빼놓지 않는다")
    void 사람을_참조하는_표를_전부_비운다() {
        given(exchangeParticipantRepository.findExchangeIdsByUserId(USER)).willReturn(List.of());
        given(exchangeItemRepository.findExchangeIdsByUserId(USER)).willReturn(List.of());

        adminCleanupService.deleteUserDeep(USER);

        verify(exchangeTimeSlotRepository).deleteAllByUserId(USER);
        verify(pokeRepository).deleteByFromUserId(USER);
        verify(pokeRepository).deleteByToUserId(USER);
        verify(notificationRepository).deleteByRecipientId(USER);
        verify(pushSubscriptionRepository).deleteByUserId(USER);
        verify(userHaveItemRepository).deleteByUserId(USER);
        verify(userWantItemRepository).deleteByUserId(USER);
    }
}
