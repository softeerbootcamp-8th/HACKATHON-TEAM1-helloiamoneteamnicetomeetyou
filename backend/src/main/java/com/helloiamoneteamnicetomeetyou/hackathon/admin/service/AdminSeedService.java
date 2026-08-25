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
 */
@Service
@RequiredArgsConstructor
public class AdminSeedService {

    /** 서비스가 지원하는 고리의 최대 인원. */
    public static final int MAX_CASE_SIZE = 3;

    private static final List<String> DUMMY_NAMES = List.of(
            "캐스퍼", "블루N", "아이오닉러버", "N드라이버", "그랜저러버",
            "포니덕후", "레몬 16", "레몬 07", "싼타페러버", "비전러버");

    /** 카드가 하나도 없는 부스에 케이스를 만들 때 같이 만들어 주는 기본 카드다. */
    private static final List<String> DEFAULT_ITEMS = List.of(
            "N Vision 74", "IONIQ 5 N", "PONY", "AVANTE N", "GRANDEUR", "SANTA FE", "CASPER");

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;

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
            userWantItemRepository.save(UserWantItem.of(seeker, owned));
        }

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
