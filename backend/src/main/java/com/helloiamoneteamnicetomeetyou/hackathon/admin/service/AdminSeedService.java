package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시연 한 판을 버튼 하나로 차린다.
 *
 * <p>부스를 이틀 열면 같은 시연을 수십 번 반복하는데, 매번 더미를 만들고 카드를 하나씩 붙이면
 * 한 판에 몇 분씩 걸린다. 관람객을 앞에 세워 두고 할 수 있는 일이 아니다.
 *
 * <p><b>더미들의 보유와 희망을 고리 모양으로 엮는다.</b> 1번이 가진 것을 2번이 찾고, 2번이 가진
 * 것을 3번이 찾고, 마지막이 1번이 가진 것을 찾는 식이다. 무작위로 뿌리면 짝이 하나도 안 나는
 * 판이 나올 수 있는데, 시연 도중에 그러면 손 쓸 방법이 없다.
 */
@Service
@RequiredArgsConstructor
public class AdminSeedService {

    private static final List<String> DUMMY_NAMES = List.of(
            "캐스퍼", "블루N", "아이오닉러버", "N드라이버", "그랜저러버",
            "포니덕후", "레몬 16", "레몬 07", "싼타페러버", "비전러버");

    /** 카드가 하나도 없는 부스에 시드를 돌렸을 때 같이 만들어 주는 기본 카드다. */
    private static final List<String> DEFAULT_ITEMS = List.of(
            "N Vision 74", "IONIQ 5 N", "PONY", "AVANTE N", "GRANDEUR", "SANTA FE", "CASPER");

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;

    /**
     * 부스 하나에 시연용 더미와 카드를 채운다.
     *
     * @param dummyCount 만들 더미 수. 고리를 만들려면 둘 이상이어야 한다.
     * @return 만든 더미 수
     */
    @Transactional
    public int seed(Long boothId, int dummyCount) {
        if (dummyCount < 2) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.BOOTH_NOT_FOUND));

        List<Item> items = ensureItems(booth);
        ensureZones(booth);

        List<User> dummies = createDummies(dummyCount);
        linkInCycle(dummies, items);

        return dummies.size();
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

    /** 약속 장소가 하나도 없으면 시연에서 장소를 고르는 화면이 비어 버린다. */
    private void ensureZones(Booth booth) {
        if (zoneRepository.countByBoothId(booth.getId()) > 0) {
            return;
        }

        zoneRepository.save(Zone.of(booth, "A 구역", "부스 입구 왼쪽"));
        zoneRepository.save(Zone.of(booth, "B 구역", "포토존 앞"));
        zoneRepository.save(Zone.of(booth, "C 구역", "휴게 테이블"));
    }

    private List<User> createDummies(int dummyCount) {
        List<User> dummies = new ArrayList<>();
        for (int index = 0; index < dummyCount; index++) {
            String name = index < DUMMY_NAMES.size()
                    ? DUMMY_NAMES.get(index)
                    : "더미 %d".formatted(index + 1);
            dummies.add(userRepository.save(User.dummy(UUID.randomUUID(), name)));
        }
        return dummies;
    }

    /**
     * 고리로 엮는다. i 번째가 가진 카드를 i-1 번째가 찾게 한다.
     *
     * <p>카드가 더미보다 적으면 같은 카드를 나눠 쓰게 되는데, 그래도 고리는 끊기지 않는다.
     */
    private void linkInCycle(List<User> dummies, List<Item> items) {
        for (int index = 0; index < dummies.size(); index++) {
            User owner = dummies.get(index);
            Item owned = items.get(index % items.size());
            userHaveItemRepository.save(UserHaveItem.of(owner, owned, 1));

            User seeker = dummies.get((index + dummies.size() - 1) % dummies.size());
            // 카드가 더미보다 적으면 앞뒤가 같은 카드를 맡게 되는 자리가 생긴다. 자기가 가진
            // 카드를 자기가 찾는 것으로 두면 화면에 말이 안 되는 줄이 남는다.
            if (userHaveItemRepository.findByUserIdAndItemId(seeker.getId(), owned.getId()).isPresent()) {
                continue;
            }
            if (!userWantItemRepository.existsByUserIdAndItemId(seeker.getId(), owned.getId())) {
                userWantItemRepository.save(UserWantItem.of(seeker, owned));
            }
        }
    }
}
