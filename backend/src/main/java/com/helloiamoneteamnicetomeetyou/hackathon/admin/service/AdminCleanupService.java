package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민에서 무언가를 지울 때 딸려 오는 것들을 걷어낸다.
 *
 * <p>지우는 자리마다 참조 정리를 각자 하고 있었더니, 표가 하나 늘 때마다 빠뜨리는 자리가
 * 생겼다. 실제로 카드를 지우면 {@code exchange_items} 에, 더미를 지우면 {@code pokes} 에 걸려서
 * 운영자에게는 이유가 안 적힌 오류 화면만 나갔다. <b>참조 관계를 아는 곳을 여기 하나로 모은다.</b>
 *
 * <p><b>순서가 규칙이다.</b> 자식 표를 먼저 비우고 부모를 지운다. 아래 메서드들은 그 순서로
 * 쓰여 있고, 새 표가 생기면 그 표를 참조 그래프에 맞는 자리에 끼워 넣는다.
 *
 * <p><b>비울 수 있는 자리는 지우지 않고 뗀다.</b> {@code Poke.exchange} 와 {@code Exchange.zone}
 * 은 없어도 되는 값이라, 거기 걸렸다고 찔러보기나 교환까지 지우면 잃는 것이 너무 많다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCleanupService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;
    private final ExchangeTimeSlotRepository exchangeTimeSlotRepository;
    private final PokeRepository pokeRepository;
    private final NotificationRepository notificationRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;

    /**
     * 교환 여러 건과 거기 딸린 것을 전부 지운다.
     *
     * <p>교환을 붙들고 있는 표가 넷이다. 찔러보기는 떼기만 하고, 고른 시간과 주고받은 카드,
     * 참가자는 그 교환 안에서만 뜻이 있어서 같이 지운다.
     */
    @Transactional
    public void deleteExchanges(List<Long> exchangeIds) {
        if (exchangeIds.isEmpty()) {
            return;
        }

        pokeRepository.detachExchanges(exchangeIds);
        exchangeTimeSlotRepository.deleteAllByExchangeIdIn(exchangeIds);
        exchangeItemRepository.deleteByExchangeIdIn(exchangeIds);
        exchangeParticipantRepository.deleteByExchangeIdIn(exchangeIds);
        exchangeRepository.deleteByIdIn(exchangeIds);

        log.info("어드민 정리: 교환 {}건을 지웠다 - {}", exchangeIds.size(), exchangeIds);
    }

    /**
     * 카드 하나를 지우면서 그 카드를 가리키는 것을 전부 걷어낸다.
     *
     * <p><b>이 카드가 오간 교환은 통째로 지운다.</b> {@code ExchangeItem.item} 은 비울 수 없는
     * 자리라, 그 줄만 지우면 무엇을 주고받았는지 모르는 반쪽짜리 교환이 남는다.
     *
     * @return 같이 지운 교환 건수. 화면이 운영자에게 알려 주는 값이다
     */
    @Transactional
    public int deleteItemDeep(Long itemId) {
        List<Long> exchangeIds = exchangeItemRepository.findExchangeIdsByItemId(itemId);
        deleteExchanges(exchangeIds);

        pokeRepository.deleteByRequestedItemId(itemId);
        pokeRepository.deleteByChosenItemId(itemId);
        userHaveItemRepository.deleteByItemId(itemId);
        userWantItemRepository.deleteByItemId(itemId);

        return exchangeIds.size();
    }

    /**
     * 사용자 하나를 지우면서 그 사람이 남긴 것을 전부 걷어낸다.
     *
     * <p>참여한 교환은 통째로 지운다. 참가자 한 명이 빠진 교환은 남은 사람 화면에서 상대 없는
     * 약속이 되어 아무것도 할 수 없다.
     *
     * @return 같이 지운 교환 건수
     */
    @Transactional
    public int deleteUserDeep(UUID userId) {
        List<Long> exchangeIds = exchangeParticipantRepository.findExchangeIdsByUserId(userId);
        deleteExchanges(exchangeIds);

        // 교환에 묶이지 않은 줄이 남아 있을 수 있다. 참가자로는 안 걸렸는데 카드만 오간
        // 경우가 그렇다. 남은 것을 여기서 확실히 없앤다.
        exchangeTimeSlotRepository.deleteAllByUserId(userId);
        pokeRepository.deleteByFromUserId(userId);
        pokeRepository.deleteByToUserId(userId);
        notificationRepository.deleteByRecipientId(userId);
        pushSubscriptionRepository.deleteByUserId(userId);
        userHaveItemRepository.deleteByUserId(userId);
        userWantItemRepository.deleteByUserId(userId);

        return exchangeIds.size();
    }

    /**
     * 구역을 지울 수 있게 약속에서 자리를 떼어 낸다.
     *
     * <p>교환까지 지우지 않는 이유는 {@code Exchange.zone} 이 비어 있어도 되는 값이기 때문이다.
     * 진행 중이던 약속은 "장소 미정" 으로 돌아가고, 운영자가 교환 탭에서 다른 자리로 옮기면 된다.
     */
    @Transactional
    public void detachZone(Long zoneId) {
        exchangeRepository.detachZone(zoneId);
    }
}
