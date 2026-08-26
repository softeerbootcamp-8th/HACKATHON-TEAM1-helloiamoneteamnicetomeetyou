package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참가자 본인이 매칭 결과에 반응한다. 대리 조작(어드민)은 {@code AdminExchangeService} 가 맡고,
 * 여기는 항상 요청을 보낸 본인 계정에 대해서만 상태를 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final SseEventPublisher sseEventPublisher;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매칭 결과를 보고 장소를 잡으러 들어간다.
     *
     * <p>상대가 아직 수락 전이어도 바로 IN_PROGRESS 로 옮긴다. 지금 화면 흐름은 양쪽이 서로의
     * 수락을 기다리지 않고 각자 장소·시간 화면으로 들어가 맞춰 보는 방식이라, "둘 다 눌러야
     * 진행 중" 같은 조건을 걸 이유가 없다.
     */
    @Transactional
    public void accept(Long exchangeId, UUID userId) {
        Exchange exchange = findExchange(exchangeId);
        findParticipant(exchangeId, userId).accept();

        if (exchange.getStatus() == ExchangeStatus.PENDING) {
            exchange.startProgress();
        }
    }

    /**
     * 매칭 결과를 거절한다. 이 교환은 참가자 전원에게 끝난 거래가 되고, 거절한 사람을 뺀
     * 나머지는 다시 상대를 찾아야 한다.
     *
     * <p>재매칭은 {@link MatchTriggerEvent} 로 미룬다. 카드 등록 때와 같은 이유다 —
     * {@code runMatching} 이 비동기라 이 트랜잭션의 커밋보다 먼저 돌면 방금 풀어 준 카드가
     * 아직 예약 중인 채로 재매칭에 잡혀 후보에서 빠진다.
     */
    @Transactional
    public void reject(Long exchangeId, UUID userId) {
        Exchange exchange = findExchange(exchangeId);
        if (exchange.getStatus() != ExchangeStatus.PENDING && exchange.getStatus() != ExchangeStatus.IN_PROGRESS) {
            return;
        }

        List<ExchangeParticipant> participants = exchangeParticipantRepository.findAllByExchangeId(exchangeId);
        findParticipant(participants, userId).reject();
        exchange.cancel();

        for (ExchangeItem item : exchangeItemRepository.findByExchangeId(exchangeId)) {
            userHaveItemRepository.findByUserIdAndItemId(item.getFromUser().getId(), item.getItem().getId())
                    .ifPresent(UserHaveItem::cancelReservation);
        }

        for (ExchangeParticipant participant : participants) {
            UUID participantId = participant.getUser().getId();
            if (!participantId.equals(userId)) {
                sseEventPublisher.toUser(participantId, SseEventType.MATCH_REJECTED, Map.of("exchangeId", exchangeId));
            }
            eventPublisher.publishEvent(new MatchTriggerEvent(participantId));
        }
    }

    /**
     * 참가자가 실물 교환을 마쳤다고 확인한다. 이 시점에 카드 수량을 실제로 옮긴다.
     *
     * <p>둘 중 아무나 한 번만 눌러도 충분하다. {@code accept} 와 같은 이유로 "둘 다 눌러야"
     * 조건을 걸지 않는다 — 실물 교환은 만나서 실제로 카드를 주고받는 행위라 한쪽이 확인하면
     * 충분하다.
     */
    @Transactional
    public void complete(Long exchangeId, UUID userId) {
        Exchange exchange = findExchange(exchangeId);
        findParticipant(exchangeId, userId);

        if (exchange.getStatus() != ExchangeStatus.IN_PROGRESS) {
            return;
        }

        for (ExchangeItem item : exchangeItemRepository.findByExchangeId(exchangeId)) {
            giveAway(item);
            receive(item);
        }

        exchange.complete();
    }

    /** 준 사람 쪽 재고를 줄인다. */
    private void giveAway(ExchangeItem item) {
        userHaveItemRepository.findByUserIdAndItemId(item.getFromUser().getId(), item.getItem().getId())
                .ifPresent(hi -> hi.completeExchange(item.getQuantity()));
    }

    /**
     * 받은 사람 쪽에 반영한다. 보유 카드는 늘리고, 그 카드를 찾고 있었으면 찾는 수량에서 뺀다.
     *
     * <p>보유 카드에 더하는 것과 재교환 가능하게 만드는 것은 다른 문제다. {@link UserHaveItem#acquired}
     * 와 {@link UserHaveItem#receiveMore} 둘 다 받은 몫을 곧바로 매칭 후보로 올리지 않는다.
     */
    private void receive(ExchangeItem item) {
        User toUser = item.getToUser();
        Item receivedItem = item.getItem();
        int quantity = item.getQuantity();

        userHaveItemRepository.findByUserIdAndItemId(toUser.getId(), receivedItem.getId())
                .ifPresentOrElse(
                        existing -> existing.receiveMore(quantity),
                        () -> userHaveItemRepository.save(UserHaveItem.acquired(toUser, receivedItem, quantity)));

        userWantItemRepository.findByUserIdAndItemId(toUser.getId(), receivedItem.getId())
                .ifPresent(want -> {
                    want.reduceQuantity(quantity);
                    if (want.getQuantity() <= 0) {
                        userWantItemRepository.delete(want);
                    }
                });
    }

    private Exchange findExchange(Long exchangeId) {
        return exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private ExchangeParticipant findParticipant(Long exchangeId, UUID userId) {
        return findParticipant(exchangeParticipantRepository.findAllByExchangeId(exchangeId), userId);
    }

    private ExchangeParticipant findParticipant(List<ExchangeParticipant> participants, UUID userId) {
        return participants.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_EXCHANGE_PARTICIPANT));
    }
}
