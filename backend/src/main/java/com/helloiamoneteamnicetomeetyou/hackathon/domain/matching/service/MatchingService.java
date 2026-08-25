package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final UserRepository userRepository;
    private final ExchangeRepository exchangeRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;
    private final ExchangeItemRepository exchangeItemRepository;

    /**
     * 1대1 교환 매칭 진행.
     * 보유·희망 아이템이 하나라도 없으면 매칭 불가.
     * 매칭 성공 시 Exchange를 생성하고 반환한다.
     * 실패 시 Optional.empty() → 호출부에서 3인 교환으로 폴백.
     */
    @Transactional
    public Optional<Exchange> runMatching(
            User myUser,
            List<UserHaveItem> myHaveItems,
            List<UserWantItem> myWantItems
    ) {
        if (myHaveItems.isEmpty() || myWantItems.isEmpty()) return Optional.empty();

        Map<Long, Long> earliestReg = new HashMap<>();
        Map<Long, Map<Long, Integer>> toThem = buildToThem(myUser.getId(), earliestReg);
        Map<Long, Map<Long, Integer>> toMe   = buildToMe(myUser.getId());

        // 두 쿼리 결과의 교집합 = 양방향 교환이 가능한 후보
        Set<Long> candidates = toThem.keySet().stream()
                .filter(toMe::containsKey)
                .collect(Collectors.toSet());
        if (candidates.isEmpty()) return Optional.empty();

        Long bestId = selectBest(candidates, toThem, toMe, earliestReg);
        return Optional.of(createExchange(myUser, bestId, myHaveItems, toThem.get(bestId), toMe.get(bestId)));
    }

    /**
     * 쿼리 A: 내 보유 아이템을 원하는 후보와 교환 가능 수량을 조회한다.
     * SQL의 JOIN + LEAST로 per-item 수량 cap까지 처리한다.
     * 결과: candidateId → { itemId → qty }
     * 부수효과: earliestReg에 후보별 최초 want 등록 ID(최솟값)를 기록한다 (동점 tiebreaker용).
     */
    private Map<Long, Map<Long, Integer>> buildToThem(Long myUserId, Map<Long, Long> earliestReg) {
        Map<Long, Map<Long, Integer>> result = new HashMap<>();
        for (Object[] row : userWantItemRepository.findToThemData(myUserId)) {
            Long candidateId = toLong(row[0]);
            Long itemId      = toLong(row[1]);
            int  qty         = toInt(row[2]);
            Long wantId      = toLong(row[3]);
            result.computeIfAbsent(candidateId, k -> new HashMap<>()).put(itemId, qty);
            earliestReg.merge(candidateId, wantId, Math::min);
        }
        return result;
    }

    /**
     * 쿼리 B: 내가 원하는 아이템을 가진 후보와 교환 가능 수량을 조회한다.
     * SQL의 JOIN + LEAST로 per-item 수량 cap까지 처리한다.
     * 결과: candidateId → { itemId → qty }
     */
    private Map<Long, Map<Long, Integer>> buildToMe(Long myUserId) {
        Map<Long, Map<Long, Integer>> result = new HashMap<>();
        for (Object[] row : userHaveItemRepository.findToMeData(myUserId)) {
            Long candidateId = toLong(row[0]);
            Long itemId      = toLong(row[1]);
            int  qty         = toInt(row[2]);
            result.computeIfAbsent(candidateId, k -> new HashMap<>()).put(itemId, qty);
        }
        return result;
    }

    /**
     * 후보 중 최적 상대를 선정한다.
     * 기준 1: score(= min(내가 줄 총량, 내가 받을 총량))가 높은 후보 우선.
     * 기준 2: score 동점이면 want 아이템을 먼저 등록한 후보 우선 (earliestReg 최솟값).
     */
    private Long selectBest(Set<Long> candidates,
                             Map<Long, Map<Long, Integer>> toThem,
                             Map<Long, Map<Long, Integer>> toMe,
                             Map<Long, Long> earliestReg) {
        return candidates.stream()
                .max(Comparator.<Long>comparingInt(id -> score(id, toThem, toMe))
                               .thenComparing(Comparator.comparingLong(
                                       (Long id) -> earliestReg.getOrDefault(id, Long.MAX_VALUE)
                               ).reversed()))
                .orElseThrow();
    }

    /**
     * Exchange, ExchangeParticipant, ExchangeItem을 생성하고 저장한다.
     * 양쪽 교환 수량을 min(toThem 합계, toMe 합계)로 맞춰 대칭 교환을 보장한다.
     * UserHaveItem.quantityLeft는 dirty checking으로 자동 반영된다.
     *
     * TODO: 3인 교환 시 참여자 3명, ExchangeItem 방향 A→B, B→C, C→A로 확장 필요.
     */
    private Exchange createExchange(User myUser, Long bestId,
                                     List<UserHaveItem> myHaveItems,
                                     Map<Long, Integer> itemsToGive,
                                     Map<Long, Integer> itemsToReceive
    ) {
        User bestUser = userRepository.findById(bestId).orElseThrow();

        Map<Long, UserHaveItem> myHaveItemMap = myHaveItems.stream()
                .filter(uhi -> itemsToGive.containsKey(uhi.getItem().getId()))
                .collect(Collectors.toMap(uhi -> uhi.getItem().getId(), uhi -> uhi, (a, b) -> a));

        Map<Long, UserHaveItem> bestHaveItemMap = userHaveItemRepository
                .findByUserIdAndItemIds(bestId, itemsToReceive.keySet()).stream()
                .collect(Collectors.toMap(uhi -> uhi.getItem().getId(), uhi -> uhi, (a, b) -> a));

        // 양쪽 총 교환 수량을 min 값으로 맞춰 대칭 보장
        int exchangeQty = Math.min(
                itemsToGive.values().stream().mapToInt(i -> i).sum(),
                itemsToReceive.values().stream().mapToInt(i -> i).sum()
        );
        Map<Long, Integer> actualGive    = capTo(itemsToGive,    exchangeQty);
        Map<Long, Integer> actualReceive = capTo(itemsToReceive, exchangeQty);

        Exchange exchange = exchangeRepository.save(Exchange.create(ExchangeType.ONE_TO_ONE));
        exchangeParticipantRepository.saveAll(List.of(
                ExchangeParticipant.create(exchange, myUser),
                ExchangeParticipant.create(exchange, bestUser)
        ));

        List<ExchangeItem> exchangeItems = new ArrayList<>();

        actualGive.forEach((itemId, qty) -> {
            UserHaveItem haveItem = myHaveItemMap.get(itemId);
            if (haveItem == null) return;
            exchangeItems.add(ExchangeItem.create(exchange, myUser, haveItem.getItem(), bestUser, qty));
            haveItem.decreaseQuantityLeft(qty);
        });

        actualReceive.forEach((itemId, qty) -> {
            UserHaveItem haveItem = bestHaveItemMap.get(itemId);
            if (haveItem == null) return;
            exchangeItems.add(ExchangeItem.create(exchange, bestUser, haveItem.getItem(), myUser, qty));
            haveItem.decreaseQuantityLeft(qty);
        });

        exchangeItemRepository.saveAll(exchangeItems);

        return exchange;
    }

    /**
     * 후보의 score를 계산한다.
     * score = min(내가 후보에게 줄 총량, 후보가 나에게 줄 총량).
     * 양방향 교환 가능한 수량의 최솟값이므로 실제 교환 규모를 나타낸다.
     */
    private int score(Long id, Map<Long, Map<Long, Integer>> toThem, Map<Long, Map<Long, Integer>> toMe) {
        int toThemTotal = toThem.getOrDefault(id, Map.of()).values().stream().mapToInt(i -> i).sum();
        int toMeTotal   = toMe.getOrDefault(id, Map.of()).values().stream().mapToInt(i -> i).sum();
        return Math.min(toThemTotal, toMeTotal);
    }

    /**
     * items를 순서대로 꺼내 총합이 limit를 초과하지 않도록 잘라낸다.
     * 양방향 교환 수량을 대칭으로 맞출 때 사용한다.
     */
    private Map<Long, Integer> capTo(Map<Long, Integer> items, int limit) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        int remaining = limit;
        for (Map.Entry<Long, Integer> e : items.entrySet()) {
            if (remaining <= 0) break;
            int take = Math.min(e.getValue(), remaining);
            result.put(e.getKey(), take);
            remaining -= take;
        }
        return result;
    }

    private Long toLong(Object o) { return ((Number) o).longValue(); }
    private int  toInt(Object o)  { return ((Number) o).intValue(); }
}
