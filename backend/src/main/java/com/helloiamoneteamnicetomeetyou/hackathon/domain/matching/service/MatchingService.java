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

import java.util.*;
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
     * 교환 매칭 진행. 1대1 매칭을 먼저 시도하고, 실패하면 3인 교환으로 폴백한다.
     *
     * 쿼리 A, B 결과를 두 맵으로 조립해 1대1, 3인 양쪽에서 재사용한다.
     *   toThem      : 후보ID → { 내가 줄 아이템ID → 교환가능수량 }  (내 보유 아이템을 원하는 상대)
     *   toMe        : 후보ID → { 내가 받을 아이템ID → 교환가능수량 }  (내가 원하는 아이템을 보유한 상대)
     *   earliestReg : 후보ID → 최소 want_id  (동점 tiebreaker — 작을수록 먼저 등록)
     */
    @Transactional
    public void runMatching(
            User myUser,
            List<UserHaveItem> myHaveItems,
            List<UserWantItem> myWantItems
    ) {
        if (myHaveItems.isEmpty() || myWantItems.isEmpty()) return;

        Map<Long, Long> earliestReg = new HashMap<>();
        Map<Long, Map<Long, Integer>> toThem = buildToThem(myUser.getId(), earliestReg);
        Map<Long, Map<Long, Integer>> toMe   = buildToMe(myUser.getId());

        tryOneToOne(myUser, myHaveItems, toThem, toMe, earliestReg)
                .or(() -> tryThreeWay(myUser, myHaveItems, toThem, toMe, earliestReg));
    }

    // ──────────────────────────────────────────
    // 1대1 교환
    // ──────────────────────────────────────────

    /**
     * 1대1 교환 매칭 시도.
     * toThem ∩ toMe 교집합이 후보이며, score가 높은 순으로 최적 상대를 선정한다.
     * 수량 대칭(capTo)까지 여기서 결정하고, createExchange는 저장만 담당한다.
     */
    private Optional<Exchange> tryOneToOne(
            User myUser,
            List<UserHaveItem> myHaveItems,
            Map<Long, Map<Long, Integer>> toThem,
            Map<Long, Map<Long, Integer>> toMe,
            Map<Long, Long> earliestReg
    ) {
        Set<Long> candidates = toThem.keySet().stream()
                .filter(toMe::containsKey)
                .collect(Collectors.toSet());
        if (candidates.isEmpty()) return Optional.empty();

        Long bestId = selectBest(candidates, toThem, toMe, earliestReg);
        Map<Long, Integer> give    = toThem.get(bestId);
        Map<Long, Integer> receive = toMe.get(bestId);
        int exchangeQty = Math.min(sum(give), sum(receive));

        return Optional.of(createExchange(myUser, bestId, myHaveItems,
                capTo(give, exchangeQty), capTo(receive, exchangeQty)));
    }

    /**
     * 후보 중 최적 상대를 선정한다.
     * 기준 1: score(= min(내가 줄 총량, 내가 받을 총량))가 높은 후보 우선.
     * 기준 2: score 동점이면 want 아이템을 먼저 등록한 후보 우선 (earliestReg 최솟값).
     */
    private Long selectBest(
            Set<Long> candidates,
            Map<Long, Map<Long, Integer>> toThem,
            Map<Long, Map<Long, Integer>> toMe,
            Map<Long, Long> earliestReg
    ) {
        return candidates.stream()
                .max(Comparator.<Long>comparingInt(id -> score(id, toThem, toMe))
                               .thenComparing(Comparator.comparingLong(
                                       (Long id) -> earliestReg.getOrDefault(id, Long.MAX_VALUE)
                               ).reversed()))
                .orElseThrow();
    }

    /**
     * 1대1 Exchange를 저장한다. give/receive는 이미 확정된 { itemId → qty }다.
     * UserHaveItem.quantityLeft는 dirty checking으로 자동 반영된다.
     */
    private Exchange createExchange(
            User myUser,
            Long bestId,
            List<UserHaveItem> myHaveItems,
            Map<Long, Integer> give,
            Map<Long, Integer> receive
    ) {
        User bestUser = userRepository.findById(bestId).orElseThrow();

        Map<Long, UserHaveItem> myMap   = indexByItemId(myHaveItems);
        Map<Long, UserHaveItem> bestMap = indexByItemId(
                userHaveItemRepository.findByUserIdAndItemIds(bestId, receive.keySet()));

        Exchange exchange = exchangeRepository.save(Exchange.create(ExchangeType.ONE_TO_ONE));
        exchangeParticipantRepository.saveAll(List.of(
                ExchangeParticipant.create(exchange, myUser),
                ExchangeParticipant.create(exchange, bestUser)));

        List<ExchangeItem> items = new ArrayList<>();
        give.forEach((itemId, qty) -> {
            UserHaveItem hi = myMap.get(itemId);
            items.add(ExchangeItem.create(exchange, myUser, hi.getItem(), bestUser, qty));
            hi.decreaseQuantityLeft(qty);
        });
        receive.forEach((itemId, qty) -> {
            UserHaveItem hi = bestMap.get(itemId);
            items.add(ExchangeItem.create(exchange, bestUser, hi.getItem(), myUser, qty));
            hi.decreaseQuantityLeft(qty);
        });
        exchangeItemRepository.saveAll(items);
        return exchange;
    }

    // ──────────────────────────────────────────
    // 3인 교환
    // ──────────────────────────────────────────

    /**
     * 3인 교환 매칭 시도. A→B→C→A 사이클을 탐색한다.
     * toThem(A→B 후보), toMe(C→A 후보)를 1대1 매칭과 공유해 쿼리를 재사용한다.
     * 추가 쿼리 1번으로 B→C 가능 여부를 조회하고 최적 (B, C) 조합을 선정한다.
     */
    private Optional<Exchange> tryThreeWay(
            User myUser,
            List<UserHaveItem> myHaveItems,
            Map<Long, Map<Long, Integer>> toThem,
            Map<Long, Map<Long, Integer>> toMe,
            Map<Long, Long> earliestReg
    ) {
        if (toThem.isEmpty() || toMe.isEmpty()) return Optional.empty();

        // 추가 쿼리: B → C (B ∈ toThem, C ∈ toMe)
        // bToC: bId → cId → { itemId → qty }
        Map<Long, Map<Long, Map<Long, Integer>>> bToC = buildBToC(toThem.keySet(), toMe.keySet());
        if (bToC.isEmpty()) return Optional.empty();

        // B는 want를 먼저 등록한 순서(earliestReg 최솟값)로 선정, C는 첫 번째
        Long bId = bToC.keySet().stream()
                .min(Comparator.comparingLong(id -> earliestReg.getOrDefault(id, Long.MAX_VALUE)))
                .orElseThrow();
        Long cId = bToC.get(bId).keySet().iterator().next();

        // 각 방향에서 교환할 아이템 1개씩 결정
        Long aToBItemId = toThem.get(bId).keySet().iterator().next();
        Long bToCItemId = bToC.get(bId).get(cId).keySet().iterator().next();
        Long cToAItemId = toMe.get(cId).keySet().iterator().next();

        return Optional.of(createThreeWayExchange(
                myUser, myHaveItems, bId, cId, aToBItemId, bToCItemId, cToAItemId
        ));
    }

    /**
     * 3인 Exchange를 저장한다. 교환할 아이템 ID는 이미 결정된 상태로 받는다.
     * UserHaveItem.quantityLeft는 dirty checking으로 자동 반영된다.
     */
    private Exchange createThreeWayExchange(
            User myUser,
            List<UserHaveItem> myHaveItems,
            Long bId,
            Long cId,
            Long aToBItemId,
            Long bToCItemId,
            Long cToAItemId
    ) {
        User userB = userRepository.findById(bId).orElseThrow();
        User userC = userRepository.findById(cId).orElseThrow();

        UserHaveItem myHaveItem = myHaveItems.stream()
                .filter(uhi -> uhi.getItem().getId().equals(aToBItemId))
                .findFirst().orElseThrow();
        UserHaveItem bHaveItem = userHaveItemRepository
                .findByUserIdAndItemIds(bId, Set.of(bToCItemId)).get(0);
        UserHaveItem cHaveItem = userHaveItemRepository
                .findByUserIdAndItemIds(cId, Set.of(cToAItemId)).get(0);

        Exchange exchange = exchangeRepository.save(Exchange.create(ExchangeType.MULTI_WAY));
        exchangeParticipantRepository.saveAll(List.of(
                ExchangeParticipant.create(exchange, myUser),
                ExchangeParticipant.create(exchange, userB),
                ExchangeParticipant.create(exchange, userC)
        ));

        exchangeItemRepository.saveAll(List.of(
                ExchangeItem.create(exchange, myUser,  myHaveItem.getItem(), userB,  1),
                ExchangeItem.create(exchange, userB,   bHaveItem.getItem(),  userC,  1),
                ExchangeItem.create(exchange, userC,   cHaveItem.getItem(),  myUser, 1)
        ));

        myHaveItem.decreaseQuantityLeft(1);
        bHaveItem.decreaseQuantityLeft(1);
        cHaveItem.decreaseQuantityLeft(1);

        return exchange;
    }

    // ──────────────────────────────────────────
    // 공통 쿼리 조립
    // ──────────────────────────────────────────

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
     * 쿼리 C (3인 전용): B가 C에게 줄 수 있는 아이템과 수량을 조회한다.
     * 결과: bId → cId → { itemId → qty }
     */
    private Map<Long, Map<Long, Map<Long, Integer>>> buildBToC(Set<Long> bIds, Set<Long> cIds) {
        Map<Long, Map<Long, Map<Long, Integer>>> result = new HashMap<>();
        for (Object[] row : userHaveItemRepository.findBToCData(bIds, cIds)) {
            Long bId    = toLong(row[0]);
            Long cId    = toLong(row[1]);
            Long itemId = toLong(row[2]);
            int  qty    = toInt(row[3]);
            result.computeIfAbsent(bId, k -> new HashMap<>())
                  .computeIfAbsent(cId, k -> new HashMap<>())
                  .put(itemId, qty);
        }
        return result;
    }

    // ──────────────────────────────────────────
    // 공통 유틸
    // ──────────────────────────────────────────

    /**
     * 1대1 후보의 score를 계산한다.
     * score = min(내가 후보에게 줄 총량, 후보가 나에게 줄 총량).
     */
    private int score(Long id, Map<Long, Map<Long, Integer>> toThem, Map<Long, Map<Long, Integer>> toMe) {
        int toThemTotal = toThem.getOrDefault(id, Map.of()).values().stream().mapToInt(i -> i).sum();
        int toMeTotal   = toMe.getOrDefault(id, Map.of()).values().stream().mapToInt(i -> i).sum();
        return Math.min(toThemTotal, toMeTotal);
    }

    /**
     * items를 순서대로 꺼내 총합이 limit를 초과하지 않도록 잘라낸다.
     * 양방향(또는 3방향) 교환 수량을 대칭으로 맞출 때 사용한다.
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

    private Map<Long, UserHaveItem> indexByItemId(List<UserHaveItem> items) {
        return items.stream().collect(
                Collectors.toMap(uhi -> uhi.getItem().getId(), uhi -> uhi, (a, b) -> a));
    }

    private int sum(Map<Long, Integer> m) {
        return m.values().stream().mapToInt(i -> i).sum();
    }

    private Long toLong(Object o) { return ((Number) o).longValue(); }
    private int  toInt(Object o)  { return ((Number) o).intValue(); }
}
