package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 부스 열기가 지켜야 하는 것은 두 가지다.
 *
 * <p>하나는 <b>어떤 조합이 들어와도 상대가 있는 것</b>이다. 관람객이 카드를 넣었는데 빈 화면이
 * 나오는 것을 막으려고 만든 기능이라, 순서쌍 하나라도 비면 만든 의미가 없다.
 *
 * <p>다른 하나는 <b>더미끼리 매칭되지 않는 것</b>이다. 매칭 트리거가 나가면 더미끼리 짝을
 * 지어 버리고, 진행 중인 교환에 낀 사람은 후보 조회에서 통째로 빠져서 정확히 반대 결과가 된다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("어드민이 부스에 대기 관람객을 세운다")
class AdminSeedServiceTest {

    private static final Long BOOTH_ID = 1L;

    /** 실제 행사 카드 수와 맞춘다. 인원 계산이 카드 종류 수에 걸려 있어서 숫자가 다르면 의미가 없다. */
    private static final int ITEM_COUNT = 9;

    @Mock
    private BoothRepository boothRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserHaveItemRepository userHaveItemRepository;
    @Mock
    private UserWantItemRepository userWantItemRepository;
    @Mock
    private ExchangeParticipantRepository exchangeParticipantRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminSeedService adminSeedService;

    private Booth booth;
    private List<Item> items;

    /** 방금 세운 사람들. 보유 카드 하나와 희망 카드 여러 장으로 들고 있는다. */
    private final List<UserHaveItem> savedHaves = new ArrayList<>();
    private final List<UserWantItem> savedWants = new ArrayList<>();

    @BeforeEach
    void setUp() {
        booth = mock(Booth.class);
        lenient().when(booth.getId()).thenReturn(BOOTH_ID);

        items = new ArrayList<>();
        for (long id = 1; id <= ITEM_COUNT; id++) {
            items.add(item(id));
        }

        given(boothRepository.findById(BOOTH_ID)).willReturn(java.util.Optional.of(booth));
        given(itemRepository.findByBoothIdOrderByIdAsc(BOOTH_ID)).willReturn(items);
        given(zoneRepository.countByBoothId(BOOTH_ID)).willReturn(3L);

        given(userRepository.save(any(User.class))).willAnswer(call -> call.getArgument(0));
        given(userHaveItemRepository.save(any(UserHaveItem.class))).willAnswer(call -> {
            UserHaveItem have = call.getArgument(0);
            savedHaves.add(have);
            return have;
        });
        given(userWantItemRepository.save(any(UserWantItem.class))).willAnswer(call -> {
            UserWantItem want = call.getArgument(0);
            savedWants.add(want);
            return want;
        });
    }

    @Test
    @DisplayName("관람객이 어떤 카드를 내놓고 어떤 카드를 찾든 상대가 한 명은 있다")
    void 모든_조합에_상대가_있다() {
        emptyBooth();

        adminSeedService.openBooth(BOOTH_ID);

        Map<UUID, Long> haveOf = haveByUser();
        Map<UUID, Set<Long>> wantsOf = wantsByUser();

        // 관람객이 X 를 내놓고 Y 를 찾을 때 1:1 이 붙으려면 Y 를 가졌고 X 를 찾는 사람이 있어야 한다.
        for (Item mine : items) {
            for (Item wanted : items) {
                if (mine.getId().equals(wanted.getId())) {
                    continue;
                }

                boolean hasPartner = haveOf.entrySet().stream().anyMatch(entry ->
                        entry.getValue().equals(wanted.getId())
                                && wantsOf.getOrDefault(entry.getKey(), Set.of()).contains(mine.getId()));

                assertThat(hasPartner)
                        .as("%d 을(를) 내놓고 %d 을(를) 찾는 관람객의 상대", mine.getId(), wanted.getId())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("세운 사람들끼리는 매칭되지 않는다")
    void 매칭_트리거를_쏘지_않는다() {
        emptyBooth();

        adminSeedService.openBooth(BOOTH_ID);

        verify(eventPublisher, never()).publishEvent(any(MatchTriggerEvent.class));
    }

    @Test
    @DisplayName("카드마다 정해진 인원만큼 세운다")
    void 카드마다_세_명씩_세운다() {
        emptyBooth();

        AdminSeedService.OpenBoothResult result = adminSeedService.openBooth(BOOTH_ID);

        assertThat(result.created()).isEqualTo(ITEM_COUNT * AdminSeedService.WAITING_PER_ITEM);
        assertThat(savedHaves).hasSize(result.created());
    }

    @Test
    @DisplayName("이미 다 채워져 있으면 새로 세우지 않는다")
    void 두_번_눌러도_늘어나지_않는다() {
        emptyBooth();
        adminSeedService.openBooth(BOOTH_ID);

        // 첫 번째로 세운 사람들이 그대로 서 있는 상태에서 다시 누른다.
        given(userHaveItemRepository.findAllWithItem()).willReturn(List.copyOf(savedHaves));
        given(userWantItemRepository.findAllWithItem()).willReturn(List.copyOf(savedWants));
        int before = savedHaves.size();

        AdminSeedService.OpenBoothResult result = adminSeedService.openBooth(BOOTH_ID);

        assertThat(result.created()).isZero();
        assertThat(savedHaves).hasSize(before);
    }

    @Test
    @DisplayName("교환에 묶인 대기자는 빠진 자리로 보고 다시 채운다")
    void 매칭된_대기자의_자리를_보충한다() {
        emptyBooth();
        adminSeedService.openBooth(BOOTH_ID);

        // 1번 카드를 들고 있던 세 명이 관람객과 매칭돼 교환에 묶였다.
        List<UUID> matched = savedHaves.stream()
                .filter(have -> have.getItem().getId().equals(1L))
                .map(have -> have.getUser().getId())
                .toList();
        given(userHaveItemRepository.findAllWithItem()).willReturn(List.copyOf(savedHaves));
        given(userWantItemRepository.findAllWithItem()).willReturn(List.copyOf(savedWants));
        given(exchangeParticipantRepository.findActiveUserIds()).willReturn(matched);

        AdminSeedService.OpenBoothResult result = adminSeedService.openBooth(BOOTH_ID);

        assertThat(matched).hasSize(AdminSeedService.WAITING_PER_ITEM);
        assertThat(result.created()).isEqualTo(AdminSeedService.WAITING_PER_ITEM);
    }

    @Test
    @DisplayName("이름이 겹치지 않는다")
    void 이름을_다시_쓰지_않는다() {
        emptyBooth();

        adminSeedService.openBooth(BOOTH_ID);

        List<String> names = savedHaves.stream().map(have -> have.getUser().getUsername()).toList();
        assertThat(names).doesNotHaveDuplicates();
    }

    /** 아무도 서 있지 않은 부스. */
    private void emptyBooth() {
        given(userHaveItemRepository.findAllWithItem()).willReturn(List.of());
        given(userWantItemRepository.findAllWithItem()).willReturn(List.of());
        given(userRepository.findAll()).willReturn(List.of());
        given(exchangeParticipantRepository.findActiveUserIds()).willReturn(List.of());
    }

    private Map<UUID, Long> haveByUser() {
        Map<UUID, Long> result = new LinkedHashMap<>();
        savedHaves.forEach(have -> result.put(have.getUser().getId(), have.getItem().getId()));
        return result;
    }

    private Map<UUID, Set<Long>> wantsByUser() {
        Map<UUID, Set<Long>> result = new LinkedHashMap<>();
        savedWants.forEach(want -> result
                .computeIfAbsent(want.getUser().getId(), id -> new LinkedHashSet<>())
                .add(want.getItem().getId()));
        return result;
    }

    private Item item(long id) {
        Item item = mock(Item.class);
        lenient().when(item.getId()).thenReturn(id);
        lenient().when(item.getBooth()).thenReturn(booth);
        return item;
    }
}
