package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto.MatchSuggestedResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;

import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ExchangeRepository exchangeRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final SseEventPublisher sseEventPublisher;

    /**
     * 교환 매칭 진행. 1대1 매칭을 먼저 시도하고, 실패하면 3인 교환으로 폴백한다.
     *
     * 쿼리 A, B 결과를 두 맵으로 조립해 1대1, 3인 양쪽에서 재사용한다.
     *   toThem      : 후보ID → { 내가 줄 아이템ID → 교환가능수량 }  (내 보유 아이템을 원하는 상대)
     *   toMe        : 후보ID → { 내가 받을 아이템ID → 교환가능수량 }  (내가 원하는 아이템을 보유한 상대)
     *   earliestReg : 후보ID → 최소 want_id  (동점 tiebreaker — 작을수록 먼저 등록)
     */
    @Async("matchingExecutor")
    @Transactional
    public void runMatching(UUID userId) {
        // 한 사람은 동시에 하나의 매칭만 가진다. 이미 진행 중인 교환이 있으면 새로 찾지 않는다.
        if (exchangeParticipantRepository.existsActiveExchange(userId)) return;

        Map<UUID, Long> earliestReg = new HashMap<>();
        Map<UUID, Map<Long, Integer>> toThem = buildToThem(userId, earliestReg);
        Map<UUID, Map<Long, Integer>> toMe   = buildToMe(userId);

        dropSelfDefeating(toThem, toMe);

        if (toThem.isEmpty() || toMe.isEmpty()) return;

        User myUser = userRepository.findById(userId).orElseThrow();
        tryOneToOne(myUser, toThem, toMe, earliestReg)
                .or(() -> tryThreeWay(myUser, toThem, toMe, earliestReg));
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
            Map<UUID, Map<Long, Integer>> toThem,
            Map<UUID, Map<Long, Integer>> toMe,
            Map<UUID, Long> earliestReg
    ) {
        Set<UUID> candidates = toThem.keySet().stream()
                .filter(toMe::containsKey)
                .collect(Collectors.toSet());
        if (candidates.isEmpty()) return Optional.empty();

        UUID bestId = selectBest(candidates, toThem, toMe, earliestReg);
        Map<Long, Integer> give    = toThem.get(bestId);
        Map<Long, Integer> receive = toMe.get(bestId);
        int exchangeQty = Math.min(sum(give), sum(receive));

        return createExchange(myUser, bestId,
                capTo(give, exchangeQty), capTo(receive, exchangeQty));
    }

    /**
     * 후보 중 최적 상대를 선정한다.
     * 기준 1: score(= min(내가 줄 총량, 내가 받을 총량))가 높은 후보 우선.
     * 기준 2: score 동점이면 want 아이템을 먼저 등록한 후보 우선 (earliestReg 최솟값).
     */
    private UUID selectBest(
            Set<UUID> candidates,
            Map<UUID, Map<Long, Integer>> toThem,
            Map<UUID, Map<Long, Integer>> toMe,
            Map<UUID, Long> earliestReg
    ) {
        return candidates.stream()
                .max(Comparator.<UUID>comparingInt(id -> score(id, toThem, toMe))
                               .thenComparing(Comparator.comparingLong(
                                       (UUID id) -> earliestReg.getOrDefault(id, Long.MAX_VALUE)
                               ).reversed()))
                .orElseThrow();
    }

    /**
     * 1대1 Exchange를 저장한다. give/receive는 이미 확정된 { itemId → qty }다.
     * 매칭 시점에는 status를 RESERVED로 변경만 한다. quantityLeft 감소는 거래 완료 시점에 처리한다.
     */
    private Optional<Exchange> createExchange(
            User myUser,
            UUID bestId,
            Map<Long, Integer> give,
            Map<Long, Integer> receive
    ) {
        if (!lockParticipants(List.of(myUser.getId(), bestId))) return Optional.empty();

        User bestUser = userRepository.findById(bestId).orElseThrow();

        Map<Long, UserHaveItem> myMap   = indexByItemId(
                userHaveItemRepository.findByUserIdAndItemIds(myUser.getId(), give.keySet()));
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
            hi.reserve();
        });
        receive.forEach((itemId, qty) -> {
            UserHaveItem hi = bestMap.get(itemId);
            items.add(ExchangeItem.create(exchange, bestUser, hi.getItem(), myUser, qty));
            hi.reserve();
        });
        exchangeItemRepository.saveAll(items);
        notifyParticipants(exchange, items, List.of(myUser, bestUser));
        return Optional.of(exchange);
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
            Map<UUID, Map<Long, Integer>> toThem,
            Map<UUID, Map<Long, Integer>> toMe,
            Map<UUID, Long> earliestReg
    ) {
        if (toThem.isEmpty() || toMe.isEmpty()) return Optional.empty();

        // 사이클의 세 다리가 같은 부스여야 한다. 다른 부스 카드가 섞이면 한 자리에 모여서
        // 교환할 수 없는 상대가 짝으로 잡힌다. 1대1 은 두 사람이 같은 카드로 이어지므로
        // (카드는 부스 하나에만 속한다) 저절로 한 부스 안이고, 사이클만 이 검사가 필요하다.
        Map<Long, Long> boothOf = boothOfItems(toThem, toMe);

        for (Long boothId : new LinkedHashSet<>(boothOf.values())) {
            Optional<Exchange> exchange =
                    tryThreeWayInBooth(myUser, boothId, boothOf, toThem, toMe, earliestReg);
            if (exchange.isPresent()) return exchange;
        }
        return Optional.empty();
    }

    /** 한 부스 안에서만 A→B→C→A 사이클을 찾는다. */
    private Optional<Exchange> tryThreeWayInBooth(
            User myUser,
            Long boothId,
            Map<Long, Long> boothOf,
            Map<UUID, Map<Long, Integer>> toThem,
            Map<UUID, Map<Long, Integer>> toMe,
            Map<UUID, Long> earliestReg
    ) {
        Map<UUID, Map<Long, Integer>> theirs = keepBooth(toThem, boothOf, boothId);
        Map<UUID, Map<Long, Integer>> mine   = keepBooth(toMe, boothOf, boothId);
        if (theirs.isEmpty() || mine.isEmpty()) return Optional.empty();

        // 추가 쿼리: B → C (B ∈ theirs, C ∈ mine)
        // bToC: bId → cId → { itemId → qty }
        Map<UUID, Map<UUID, Map<Long, Integer>>> bToC = buildBToC(boothId, theirs.keySet(), mine.keySet());
        if (bToC.isEmpty()) return Optional.empty();

        // B는 want를 먼저 등록한 순서(earliestReg 최솟값)로 선정, C는 첫 번째
        UUID bId = bToC.keySet().stream()
                .min(Comparator.comparingLong(id -> earliestReg.getOrDefault(id, Long.MAX_VALUE)))
                .orElseThrow();
        UUID cId = bToC.get(bId).keySet().iterator().next();

        // 각 방향에서 교환할 아이템 1개씩 결정
        Long aToBItemId = theirs.get(bId).keySet().iterator().next();
        Long bToCItemId = bToC.get(bId).get(cId).keySet().iterator().next();

        // 내가 내놓은 카드를 사이클을 한 바퀴 돌아 도로 받으면 교환이 아니다. 다른 카드를 찾고,
        // 없으면 이 조합은 포기한다. 1대1 쪽 dropSelfDefeating 과 같은 이유다.
        Long cToAItemId = mine.get(cId).keySet().stream()
                .filter(itemId -> !itemId.equals(aToBItemId))
                .findFirst()
                .orElse(null);
        if (cToAItemId == null) return Optional.empty();

        return createThreeWayExchange(
                myUser, bId, cId, aToBItemId, bToCItemId, cToAItemId
        );
    }

    /** 두 후보 맵에 등장하는 카드가 각각 어느 부스 것인지 한 번에 읽는다. */
    private Map<Long, Long> boothOfItems(
            Map<UUID, Map<Long, Integer>> toThem,
            Map<UUID, Map<Long, Integer>> toMe
    ) {
        Set<Long> itemIds = new LinkedHashSet<>();
        toThem.values().forEach(m -> itemIds.addAll(m.keySet()));
        toMe.values().forEach(m -> itemIds.addAll(m.keySet()));
        if (itemIds.isEmpty()) return Map.of();

        Map<Long, Long> result = new LinkedHashMap<>();
        for (Object[] row : itemRepository.findBoothIdsByItemIds(itemIds)) {
            result.put(toLong(row[0]), toLong(row[1]));
        }
        return result;
    }

    /** 후보 맵에서 이 부스 카드만 남긴다. 남는 카드가 없어진 후보는 통째로 뺀다. */
    private Map<UUID, Map<Long, Integer>> keepBooth(
            Map<UUID, Map<Long, Integer>> source,
            Map<Long, Long> boothOf,
            Long boothId
    ) {
        Map<UUID, Map<Long, Integer>> result = new LinkedHashMap<>();
        source.forEach((candidateId, items) -> {
            Map<Long, Integer> kept = new LinkedHashMap<>();
            items.forEach((itemId, qty) -> {
                if (boothId.equals(boothOf.get(itemId))) kept.put(itemId, qty);
            });
            if (!kept.isEmpty()) result.put(candidateId, kept);
        });
        return result;
    }

    /**
     * 3인 Exchange를 저장한다. 교환할 아이템 ID는 이미 결정된 상태로 받는다.
     * 매칭 시점에는 status를 RESERVED로 변경만 한다. quantityLeft 감소는 거래 완료 시점에 처리한다.
     */
    private Optional<Exchange> createThreeWayExchange(
            User myUser,
            UUID bId,
            UUID cId,
            Long aToBItemId,
            Long bToCItemId,
            Long cToAItemId
    ) {
        if (!lockParticipants(List.of(myUser.getId(), bId, cId))) return Optional.empty();

        User userB = userRepository.findById(bId).orElseThrow();
        User userC = userRepository.findById(cId).orElseThrow();

        UserHaveItem myHaveItem = userHaveItemRepository
                .findByUserIdAndItemIds(myUser.getId(), Set.of(aToBItemId)).get(0);
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

        List<ExchangeItem> items = List.of(
                ExchangeItem.create(exchange, myUser, myHaveItem.getItem(), userB,  1),
                ExchangeItem.create(exchange, userB,  bHaveItem.getItem(),  userC,  1),
                ExchangeItem.create(exchange, userC,  cHaveItem.getItem(),  myUser, 1)
        );
        exchangeItemRepository.saveAll(items);

        myHaveItem.reserve();
        bHaveItem.reserve();
        cHaveItem.reserve();

        notifyParticipants(exchange, items, List.of(myUser, userB, userC));
        return Optional.of(exchange);
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
    private Map<UUID, Map<Long, Integer>> buildToThem(UUID myUserId, Map<UUID, Long> earliestReg) {
        Map<UUID, Map<Long, Integer>> result = new LinkedHashMap<>();
        for (Object[] row : userWantItemRepository.findToThemData(myUserId.toString())) {
            UUID candidateId = toUUID(row[0]);
            Long itemId      = toLong(row[1]);
            int  qty         = toInt(row[2]);
            Long wantId      = toLong(row[3]);
            result.computeIfAbsent(candidateId, k -> new LinkedHashMap<>()).put(itemId, qty);
            earliestReg.merge(candidateId, wantId, Math::min);
        }
        return result;
    }

    /**
     * 쿼리 B: 내가 원하는 아이템을 가진 후보와 교환 가능 수량을 조회한다.
     * SQL의 JOIN + LEAST로 per-item 수량 cap까지 처리한다.
     * 결과: candidateId → { itemId → qty }
     */
    private Map<UUID, Map<Long, Integer>> buildToMe(UUID myUserId) {
        Map<UUID, Map<Long, Integer>> result = new LinkedHashMap<>();
        for (Object[] row : userHaveItemRepository.findToMeData(myUserId.toString())) {
            UUID candidateId = toUUID(row[0]);
            Long itemId      = toLong(row[1]);
            int  qty         = toInt(row[2]);
            result.computeIfAbsent(candidateId, k -> new LinkedHashMap<>()).put(itemId, qty);
        }
        return result;
    }

    /**
     * 쿼리 C (3인 전용): B가 C에게 줄 수 있는 아이템과 수량을 조회한다.
     * 결과: bId → cId → { itemId → qty }
     */
    private Map<UUID, Map<UUID, Map<Long, Integer>>> buildBToC(Long boothId, Set<UUID> bIds, Set<UUID> cIds) {
        Map<UUID, Map<UUID, Map<Long, Integer>>> result = new LinkedHashMap<>();
        for (Object[] row : userHaveItemRepository.findBToCData(boothId, toStrings(bIds), toStrings(cIds))) {
            UUID bId    = toUUID(row[0]);
            UUID cId    = toUUID(row[1]);
            Long itemId = toLong(row[2]);
            int  qty    = toInt(row[3]);
            result.computeIfAbsent(bId, k -> new LinkedHashMap<>())
                  .computeIfAbsent(cId, k -> new LinkedHashMap<>())
                  .put(itemId, qty);
        }
        return result;
    }

    // ──────────────────────────────────────────
    // 공통 유틸
    // ──────────────────────────────────────────

    /**
     * 교환을 만들기 직전에 참가자 전원을 잠그고, 그 사이 누가 다른 매칭에 묶이지 않았는지 다시 본다.
     *
     * <p>{@code runMatching} 첫 줄의 {@code existsActiveExchange} 검사와 실제 저장 사이에는 락이
     * 없다. 매칭은 스레드 4개로 동시에 돌기 때문에, 두 사람의 매칭이 서로를 후보로 잡으면 양쪽 다
     * 검사를 통과하고 교환을 두 건 만든다. 그 사이를 막는 자리가 여기다.
     *
     * <p>UUID 오름차순으로 잠근다. 순서를 고정하지 않으면 두 스레드가 서로가 쥔 행을 기다려
     * 교착에 빠진다.
     *
     * @return 전원이 아직 비어 있어 이 교환을 만들어도 되면 true
     */
    private boolean lockParticipants(List<UUID> userIds) {
        List<UUID> ordered = userIds.stream().sorted().toList();

        for (UUID userId : ordered) {
            userRepository.findByIdForUpdate(userId).orElseThrow();
        }
        return ordered.stream().noneMatch(exchangeParticipantRepository::existsActiveExchange);
    }

    /**
     * 같은 카드를 주면서 동시에 받는 조합을 후보에서 걷어낸다.
     *
     * <p>내가 카드 X 를 보유와 희망 양쪽에 등록해 두면(등록 API 가 이제 막지만 예전에 쌓인 행이
     * 남아 있다) 상대 하나가 toThem 과 toMe 양쪽에 X 로 잡힌다. 그대로 두면 X 를 건네고 X 를
     * 돌려받는 교환이 성사돼 양쪽 카드가 예약만 되고 아무것도 달라지지 않는다.
     *
     * <p>score 계산 전에 걷어내야 한다. 남겨 두면 의미 없는 수량이 점수를 부풀려 엉뚱한 상대가
     * 최적으로 뽑힌다.
     */
    private void dropSelfDefeating(
            Map<UUID, Map<Long, Integer>> toThem,
            Map<UUID, Map<Long, Integer>> toMe
    ) {
        for (UUID candidateId : Set.copyOf(toThem.keySet())) {
            Map<Long, Integer> give    = toThem.get(candidateId);
            Map<Long, Integer> receive = toMe.get(candidateId);
            if (receive == null) continue;

            Set<Long> both = new HashSet<>(give.keySet());
            both.retainAll(receive.keySet());
            if (both.isEmpty()) continue;

            both.forEach(give::remove);
            both.forEach(receive::remove);

            if (give.isEmpty())    toThem.remove(candidateId);
            if (receive.isEmpty()) toMe.remove(candidateId);
        }
    }

    /**
     * 1대1 후보의 score를 계산한다.
     * score = min(내가 후보에게 줄 총량, 후보가 나에게 줄 총량).
     */
    private int score(UUID id, Map<UUID, Map<Long, Integer>> toThem, Map<UUID, Map<Long, Integer>> toMe) {
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

    /**
     * 아직 수락하지 않은 매칭 제안. 없으면 {@code null} 이다.
     *
     * <p>실시간 연결이 붙을 때 부른다. 매칭 제안은 {@code MATCH_SUGGESTED} 로만 나가고 끊겼던
     * 동안의 이벤트는 다시 오지 않기 때문에, 이게 없으면 재연결한 사람이 자기에게 온 제안을
     * 영영 못 본다.
     *
     * <p>{@code GET /api/exchanges/active} 로는 대신할 수 없다. 그쪽은 자리와 시간이 잡힌
     * 약속만 돌려주려고 제안 단계의 교환을 일부러 걸러낸다. 여기는 정확히 그 반대를 본다.
     *
     * <p>돌려주는 것은 {@code MATCH_SUGGESTED} 이벤트와 같은 payload 다. 화면이 실시간으로 받은
     * 것과 다시 읽은 것을 같은 코드로 처리할 수 있어야 한다.
     */
    public MatchSuggestedResponseDto findPendingSuggestionOf(UUID userId) {
        User viewer = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        return exchangeRepository.findActiveByUserId(userId, List.of(ExchangeStatus.PENDING)).stream()
                // 자리와 시간이 붙은 것은 이미 수락해서 약속으로 넘어간 교환이다. 그건 약속 화면이
                // GET /api/exchanges/active 로 가져간다.
                .filter(exchange -> !exchange.hasAppointment())
                .map(exchange -> toSuggestion(exchange, viewer))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 제안 payload 를 만든다. 이 사람이 주거나 받을 것이 없으면 {@code null} 이다.
     *
     * <p>주고받을 것이 한쪽이라도 없는 교환은 제안으로 성립하지 않는다. 그런 데이터가 남아 있으면
     * payload 를 만들다 터지는데, 화면이 500 을 받는 것보다 제안이 없다고 보는 편이 맞다.
     */
    private MatchSuggestedResponseDto toSuggestion(Exchange exchange, User viewer) {
        List<ExchangeItem> items = exchangeItemRepository.findByExchangeId(exchange.getId());

        boolean gives = items.stream().anyMatch(item -> item.getFromUser().getId().equals(viewer.getId()));
        boolean receives = items.stream().anyMatch(item -> item.getToUser().getId().equals(viewer.getId()));

        if (!gives || !receives) {
            log.warn("주고받을 것이 없는 교환을 제안에서 건너뛴다: exchangeId={}, userId={}",
                    exchange.getId(), viewer.getId());
            return null;
        }

        return MatchSuggestedResponseDto.of(exchange, items, viewer);
    }

    /**
     * 참여자 한 명당 한 번, 그 사람 관점으로 정리한 매칭 결과를 보낸다.
     *
     * <p>같은 교환이라도 사람마다 주는 카드와 받는 카드가 다르다. 전원에게 같은 목록을 보내고
     * 각자 걸러 쓰게 하면 그 해석이 클라이언트마다 다시 구현된다.
     */
    private void notifyParticipants(Exchange exchange, List<ExchangeItem> items, List<User> participants) {
        for (User participant : participants) {
            sseEventPublisher.toUser(
                    participant.getId(),
                    SseEventType.MATCH_SUGGESTED,
                    MatchSuggestedResponseDto.of(exchange, items, participant));
        }
    }

    /** 네이티브 쿼리의 user_id 는 varchar(36) 이라 UUID 가 아니라 문자열로 넘겨야 한다. */
    private Set<String> toStrings(Set<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.toSet());
    }

    private UUID toUUID(Object o) { return UUID.fromString(o.toString()); }
    private Long toLong(Object o) { return ((Number) o).longValue(); }
    private int  toInt(Object o)  { return ((Number) o).intValue(); }
}
