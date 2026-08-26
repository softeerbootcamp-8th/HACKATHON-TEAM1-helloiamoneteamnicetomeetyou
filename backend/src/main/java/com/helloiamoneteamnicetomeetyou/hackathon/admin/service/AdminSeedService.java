package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.ItemStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매칭 케이스를 통째로 만든다.
 *
 * <p>부스를 이틀 열면 같은 시연을 수십 번 반복하는데, 매번 사람을 하나씩 만들고 카드를 하나씩
 * 붙이면 관람객을 앞에 세워 둔 채로 몇 분을 쓰게 된다.
 *
 * <p><b>카드는 고리로 엮는다.</b> 2인이면 서로 맞바꾸고, 3인이면 A 가 가진 것을 B 가 찾고
 * B 가 가진 것을 C 가, C 가 가진 것을 A 가 찾는다. 무작위로 뿌리면 짝이 하나도 안 나는 판이
 * 나올 수 있는데, 시연 도중에 그러면 손 쓸 방법이 없다.
 *
 * <p><b>4인 이상 고리는 만들지 않는다.</b> 서비스가 지원하는 매칭이 3인까지라, 여기서 4인
 * 고리를 만들면 화면이 처리하지 못하는 데이터가 DB 에 들어간다.
 *
 * <p>여기에는 성격이 다른 두 가지가 있다. {@link #createCases} 는 <b>이미 성사된</b> 교환을
 * 만들어 교환 진행 화면을 시연하는 것이고, {@link #openBooth} 는 <b>아직 짝이 없는</b> 사람들을
 * 부스에 세워 두어 관람객이 들어왔을 때 매칭이 붙게 하는 것이다. 둘을 헷갈리면 안 된다 —
 * 케이스로 만든 더미는 이미 짝이 있어서 관람객의 후보에서 통째로 빠진다.
 */
@Service
@RequiredArgsConstructor
public class AdminSeedService {

    /** 서비스가 지원하는 고리의 최대 인원. */
    public static final int MAX_CASE_SIZE = 3;

    /**
     * 카드 한 종을 몇 명이 들고 대기하게 할지.
     *
     * <p><b>커버리지를 정하는 값이 아니다.</b> 관람객이 {@code X 를 내놓고 Y 를 찾는다} 고
     * 넣었을 때 1:1 이 붙으려면 <b>Y 를 가졌고 X 를 찾는</b> 사람이 있어야 하는데, 그건 Y 를 든
     * 대기자들에게 <b>안 덮인 카드를 남김없이 나눠 주는</b> {@link #share} 가 보장한다.
     * 이 숫자는 그 몫을 몇 사람이 나눠 지는지를 정할 뿐이다.
     *
     * <p>그래서 3인 이유는 다른 데 있다. 카드가 9종이면 한 사람이 희망 카드를 세 장쯤 들게 되어
     * 전체리스트에 뜨는 모습이 사람처럼 보이고, 한 명이 관람객과 매칭돼 빠져도 그 카드 자리에
     * 두 명이 남는다. 2명이면 한 사람당 네 장을 지면서 여유가 한 명뿐이고, 4명 이상은 사람만
     * 늘고 덮이는 카드는 그대로다.
     */
    public static final int WAITING_PER_ITEM = 3;

    /** 대기자가 내놓는 장수. 한 장이면 첫 교환에 바로 바닥나서 그 카드 자리가 빈다. */
    private static final int WAITING_QUANTITY = 2;

    /** 덮이지 않은 카드가 없을 때 새 대기자에게 붙여 주는 희망 카드 장수. */
    private static final int FILLER_WANT_COUNT = 3;

    /**
     * 더미에게 붙일 이름이다.
     *
     * <p><b>부스를 채우는 데 필요한 만큼은 있어야 한다.</b> 모자라면 {@code 캐스퍼 2} 처럼 번호가
     * 붙는데, 관람객 화면에 그대로 보이는 이름이라 더미인 게 바로 티가 난다.
     */
    private static final List<String> DUMMY_NAMES = List.of(
            "캐스퍼", "블루N", "아이오닉러버", "N드라이버", "그랜저러버",
            "포니덕후", "레몬 16", "레몬 07", "싼타페러버", "비전러버",
            "코나킴", "벨로스터박", "아반떼사랑", "i30오너", "랠리덕후",
            "투싼러버", "스타리아", "팰리세이드", "제네시스박", "포니정",
            "N라인", "트랙데이", "서킷러버", "머플러소리", "붉은뿔",
            "다크나이트", "화이트펄", "블랙에디션", "카본휠", "퍼포먼스킴",
            "주행거리0", "첫차", "오너클럽", "주말드라이버", "한정판사냥꾼");

    /** 카드가 하나도 없는 부스에 케이스를 만들 때 같이 만들어 주는 기본 카드다. */
    private static final List<String> DEFAULT_ITEMS = List.of(
            "IONIQ 5 N",
            "AVANTE N",
            "VELOSTER N",
            "KONA N",
            "i30 N",
            "i30 Fastback",
            "i20 N",
            "AVANTE N Facelift",
            "i20 N Rally1");

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매칭 케이스를 만든다.
     *
     * @param caseSize 한 고리에 몇 명이 들어가는지. 2 또는 3만 된다
     * @param caseCount 같은 크기의 고리를 몇 개 만들지
     * @return 만들어진 사용자 수
     */
    @Transactional
    public int createCases(Long boothId, int caseSize, int caseCount) {
        if (caseSize < 2 || caseSize > MAX_CASE_SIZE) {
            throw new ApplicationException(ErrorCode.UNSUPPORTED_MATCHING_SIZE);
        }
        if (caseCount < 1) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.BOOTH_NOT_FOUND));

        List<Item> items = ensureItems(booth);
        ensureZones(booth);

        int created = 0;
        for (int index = 0; index < caseCount; index++) {
            created += createOneCase(items, caseSize, index * caseSize);
        }
        return created;
    }

    /**
     * 부스를 연다. 아직 짝이 없는 대기 관람객을 카드 종류마다 {@link #WAITING_PER_ITEM} 명까지
     * 채운다.
     *
     * <p><b>{@link MatchTriggerEvent} 를 쏘지 않는다. 이 메서드의 핵심이다.</b>
     * {@code MatchingService.runMatching} 은 이벤트를 받은 <b>그 사람 기준으로만</b> 돌고,
     * 더미는 후보로 끌려오기만 한다. 그래서 트리거를 쏘지 않으면 여기서 만든 사람들끼리는
     * 서로 짝이 맞아떨어져도 매칭되지 않고 계속 대기 상태로 남아 있다가, 진짜 관람객이 카드를
     * 등록하는 순간 그 사람에게 끌려온다.
     *
     * <p>반대로 {@link #createCases} 처럼 트리거를 쏘면 더미끼리 즉시 매칭돼서 {@code PENDING}
     * 교환에 들어가는데, 후보 조회 쿼리가 진행 중인 교환에 낀 사람을 전부 제외하기 때문에
     * (<code>findToThemData</code>, <code>findToMeData</code> 의 {@code NOT IN} 절)
     * 미리 세워 둔 더미가 관람객의 후보에서 통째로 빠진다. 부스를 열어 두려고 만든 데이터가
     * 정확히 반대 결과를 내는 셈이라, 두 메서드를 섞으면 안 된다.
     *
     * <p><b>모자란 만큼만 채운다.</b> 관람객과 매칭된 대기자는 교환에 묶여 후보에서 빠지므로
     * 하루가 지나면 자리가 비는데, 다시 눌러 빈 자리만 보충할 수 있어야 한다. 이미 서 있는
     * 사람이 덮고 있는 희망 카드는 다시 만들지 않고 <b>비어 있는 조합만</b> 메운다.
     *
     * @return 새로 만든 대기자 수와 메운 조합 수
     */
    @Transactional
    public OpenBoothResult openBooth(Long boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.BOOTH_NOT_FOUND));

        List<Item> items = ensureItems(booth);
        // 카드가 한 종뿐이면 서로 같은 카드만 들고 있어서 교환 자체가 성립하지 않는다.
        if (items.size() < 2) {
            throw new ApplicationException(ErrorCode.ITEM_NOT_FOUND);
        }
        ensureZones(booth);

        Map<Long, Item> itemById = items.stream()
                .collect(Collectors.toMap(Item::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<Long> allItemIds = List.copyOf(itemById.keySet());

        Set<UUID> busy = Set.copyOf(exchangeParticipantRepository.findActiveUserIds());
        Map<Long, List<UUID>> waitingByItem = waitingByItem(booth.getId(), busy);
        Map<UUID, Set<Long>> wantsByUser = wantsByUser();
        Set<String> takenNames = takenNames();

        int created = 0;
        int filledCombos = 0;
        int fillerCursor = 0;

        for (Item target : items) {
            List<UUID> waiting = waitingByItem.getOrDefault(target.getId(), List.of());

            // 이미 서 있는 사람들이 덮고 있는 희망 카드. 이건 다시 만들 필요가 없다.
            Set<Long> covered = waiting.stream()
                    .flatMap(userId -> wantsByUser.getOrDefault(userId, Set.of()).stream())
                    .collect(Collectors.toSet());
            List<Long> missing = allItemIds.stream()
                    .filter(itemId -> !itemId.equals(target.getId()) && !covered.contains(itemId))
                    .toList();

            /*
              인원이 모자라면 그만큼, 인원은 찼는데 안 덮인 카드가 남았으면 최소 한 명은 세운다.
              커버리지가 목적이라 사람 수보다 구멍이 없는 쪽이 먼저다 — 세 명이 서 있어도 그
              셋의 희망이 겹쳐 있으면 어떤 관람객에게는 여전히 상대가 없다.
            */
            int shortfall = WAITING_PER_ITEM - waiting.size();
            int newcomers = Math.max(shortfall, missing.isEmpty() ? 0 : 1);
            if (newcomers <= 0) {
                continue;
            }

            for (List<Long> share : share(missing, newcomers)) {
                List<Long> wants = share.isEmpty()
                        ? filler(allItemIds, target.getId(), fillerCursor++)
                        : share;
                createWaiting(target, wants, itemById, takenNames);
                created++;
            }
            filledCombos += missing.size();
        }

        return new OpenBoothResult(created, filledCombos);
    }

    /** {@link #openBooth} 의 결과. 운영자에게 무엇이 채워졌는지 알려 주려고 둘을 나눠 센다. */
    public record OpenBoothResult(int created, int filledCombos) {}

    /**
     * 대기자 한 명을 세운다. 보유 카드 한 장과 희망 카드 몇 장이 전부다.
     *
     * <p>매칭 트리거를 쏘지 않는 이유는 {@link #openBooth} 에 적어 두었다.
     */
    private void createWaiting(
            Item target, List<Long> wantItemIds, Map<Long, Item> itemById, Set<String> takenNames) {

        User user = userRepository.save(User.dummy(UUID.randomUUID(), nextName(takenNames)));
        userHaveItemRepository.save(UserHaveItem.of(user, target, WAITING_QUANTITY));
        wantItemIds.forEach(itemId ->
                userWantItemRepository.save(UserWantItem.of(user, itemById.get(itemId), 1)));
    }

    /**
     * 카드별로 지금 대기 중인 더미가 누구인지 모은다.
     *
     * <p>대기 중이란 <b>더미이고, 그 카드를 아직 내놓고 있고, 진행 중인 교환에 끼어 있지 않은</b>
     * 것이다. 셋 중 하나라도 어긋나면 관람객의 후보에 뜨지 않아서, 서 있는 사람으로 세면 안 된다.
     *
     * <p>보유 카드를 통째로 읽어 메모리에서 나눈다. 부스 규모에서는 이 편이 싸다.
     */
    private Map<Long, List<UUID>> waitingByItem(Long boothId, Set<UUID> busy) {
        Map<Long, List<UUID>> result = new LinkedHashMap<>();

        for (UserHaveItem have : userHaveItemRepository.findAllWithItem()) {
            if (!have.getUser().isAdminManaged()
                    || !Objects.equals(have.getItem().getBooth().getId(), boothId)
                    || have.getStatus() != ItemStatus.LEFT
                    || have.getQuantityLeft() == null
                    || have.getQuantityLeft() <= 0
                    || busy.contains(have.getUser().getId())) {
                continue;
            }

            result.computeIfAbsent(have.getItem().getId(), id -> new ArrayList<>())
                    .add(have.getUser().getId());
        }

        return result;
    }

    /** 사용자별 희망 카드. 대기자가 무엇을 덮고 있는지 보는 데 쓴다. */
    private Map<UUID, Set<Long>> wantsByUser() {
        Map<UUID, Set<Long>> result = new LinkedHashMap<>();

        for (UserWantItem want : userWantItemRepository.findAllWithItem()) {
            result.computeIfAbsent(want.getUser().getId(), id -> new LinkedHashSet<>())
                    .add(want.getItem().getId());
        }

        return result;
    }

    /**
     * 안 덮인 카드를 대기자들에게 돌아가며 나눈다.
     *
     * <p>몫이 사람 수보다 적으면 뒤쪽 사람은 빈손이 되는데, 부르는 쪽이 그때 기본 희망 카드를
     * 붙여 준다. 여기서 채워 버리면 "구멍을 메운 것" 과 "그냥 세운 것" 을 구분할 수 없게 된다.
     */
    private List<List<Long>> share(List<Long> missing, int people) {
        List<List<Long>> shares = new ArrayList<>();
        for (int i = 0; i < people; i++) {
            shares.add(new ArrayList<>());
        }

        for (int i = 0; i < missing.size(); i++) {
            shares.get(i % people).add(missing.get(i));
        }

        return shares;
    }

    /**
     * 덮을 구멍이 없는 대기자의 희망 카드.
     *
     * <p>희망 카드가 하나도 없으면 전체리스트에서 "무엇을 찾는지" 칸이 비어 사람이 아닌 티가
     * 난다. {@code cursor} 로 시작 위치를 옮겨서 새로 세우는 사람마다 다른 카드를 찾게 한다.
     */
    private List<Long> filler(List<Long> allItemIds, Long excluded, int cursor) {
        List<Long> others = allItemIds.stream().filter(id -> !id.equals(excluded)).toList();

        List<Long> picked = new ArrayList<>();
        for (int i = 0; i < FILLER_WANT_COUNT && i < others.size(); i++) {
            picked.add(others.get((cursor * FILLER_WANT_COUNT + i) % others.size()));
        }

        return List.copyOf(new LinkedHashSet<>(picked));
    }

    /** 이미 쓰이고 있는 이름. 같은 이름이 둘 있으면 어드민에서 누구를 고르는지 알 수 없다. */
    private Set<String> takenNames() {
        return userRepository.findAll().stream()
                .map(User::getUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 아직 안 쓴 이름을 하나 꺼낸다. 목록을 다 쓰면 번호를 붙인다. */
    private String nextName(Set<String> takenNames) {
        for (String name : DUMMY_NAMES) {
            if (takenNames.add(name)) {
                return name;
            }
        }

        for (int suffix = 2; ; suffix++) {
            for (String name : DUMMY_NAMES) {
                String candidate = "%s %d".formatted(name, suffix);
                if (takenNames.add(candidate)) {
                    return candidate;
                }
            }
        }
    }

    /**
     * 고리 하나를 만든다.
     *
     * <p>케이스마다 다른 카드를 쓴다. 같은 카드로 여러 고리를 만들면 서로 다른 케이스의 사람이
     * 섞여서 매칭되어, 시연에서 보여 주려던 구도가 아닌 짝이 나온다.
     */
    private int createOneCase(List<Item> items, int caseSize, int offset) {
        List<User> members = new ArrayList<>();
        for (int i = 0; i < caseSize; i++) {
            members.add(userRepository.save(User.dummy(UUID.randomUUID(), nameAt(offset + i))));
        }

        for (int i = 0; i < caseSize; i++) {
            Item owned = items.get((offset + i) % items.size());
            userHaveItemRepository.save(UserHaveItem.of(members.get(i), owned, 1));

            // 앞사람이 내 카드를 찾게 한다. 마지막 사람의 카드는 첫 사람이 찾아서 고리가 닫힌다.
            User seeker = members.get((i + caseSize - 1) % caseSize);
            userWantItemRepository.save(UserWantItem.of(seeker, owned, 1));
        }

        // 고리를 다 엮은 뒤에 한 번씩 돌린다. 카드를 붙이는 중간에 돌리면 아직 상대가 찾는
        // 카드를 등록하기 전이라 후보가 없다고 판단하고 그냥 지나간다.
        //
        // 이걸 안 하면 케이스를 만들어 놓고도 매칭이 붙지 않아서, 부스에서 "더미는 만들어졌는데
        // 매칭이 안 뜬다" 가 된다. 리스너가 AFTER_COMMIT 이라 실제로는 이 트랜잭션이 끝난 뒤에 돈다.
        members.forEach(member -> eventPublisher.publishEvent(new MatchTriggerEvent(member.getId())));

        return caseSize;
    }

    /** 이름 목록을 다 쓰면 번호를 붙인다. */
    private String nameAt(int index) {
        if (index < DUMMY_NAMES.size()) {
            return DUMMY_NAMES.get(index);
        }
        return "%s %d".formatted(DUMMY_NAMES.get(index % DUMMY_NAMES.size()), index / DUMMY_NAMES.size() + 1);
    }

    /** 카드가 없으면 기본 카드를 만들어 준다. 이미 있으면 그대로 쓴다. */
    private List<Item> ensureItems(Booth booth) {
        List<Item> items = itemRepository.findByBoothIdOrderByIdAsc(booth.getId());
        if (!items.isEmpty()) {
            return items;
        }

        return DEFAULT_ITEMS.stream()
                .map(name -> itemRepository.save(Item.of(booth, name, null, null)))
                .toList();
    }

    /** 만날 자리가 하나도 없으면 시연에서 장소를 고르는 화면이 비어 버린다. */
    private void ensureZones(Booth booth) {
        if (zoneRepository.countByBoothId(booth.getId()) > 0) {
            return;
        }

        zoneRepository.save(Zone.of(booth, "A 구역", "부스 입구 왼쪽"));
        zoneRepository.save(Zone.of(booth, "B 구역", "포토존 앞"));
        zoneRepository.save(Zone.of(booth, "C 구역", "휴게 테이블"));
    }
}
