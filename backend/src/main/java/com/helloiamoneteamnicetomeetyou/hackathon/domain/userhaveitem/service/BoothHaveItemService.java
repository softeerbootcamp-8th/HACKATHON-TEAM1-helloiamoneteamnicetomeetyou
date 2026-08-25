package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto.BoothHaveItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageRequestValues;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부스 안에서 다른 사람들이 내놓은 카드를 한 화면에 모아 준다. 교환 대기장소의 전체 리스트다.
 *
 * <p>등록은 {@link UserHaveItemService} 가 하고 여기는 조회만 한다. 한 클래스에 두지 않은 것은
 * 이 조회가 희망 카드와 부스까지 끌어와서, 등록만 하는 쪽이 필요 없는 의존을 갖게 되기 때문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothHaveItemService {

    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final UserRepository userRepository;
    private final BoothRepository boothRepository;

    /**
     * 나를 뺀 다른 사용자의 보유 카드를 정렬해 한 페이지 내려준다.
     *
     * <p><b>필터가 아니라 정렬이다.</b> 내 희망 카드를 위로 올리지만 나머지도 아래에 전부 나온다.
     * 원하는 카드가 없어도 화면이 비지 않고, 눈으로 보고 마음이 바뀌는 경우를 막지 않는다.
     *
     * <p>순서는 내 희망 카드 → 교환이 바로 성립하는 것(줄 수 있는 카드가 있음) →
     * {@code haveItemId} 오름차순이다. <b>마지막 기준이 없으면 안 된다.</b> 앞의 두 기준이 같은
     * 행끼리 순서가 매번 달라져서, 페이지를 넘길 때 같은 줄이 두 번 나오거나 빠진다.
     */
    public PageResponse<BoothHaveItemResponseDto> findByBooth(
            Long boothId, UUID userId, int page, int size) {

        // Bean Validation 이 아직 없어서 형식 검증을 여기서 한다.
        if (boothId == null || userId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }
        if (!boothRepository.existsById(boothId)) {
            throw new ApplicationException(ErrorCode.BOOTH_NOT_FOUND);
        }
        if (!userRepository.existsById(userId)) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }

        List<UserHaveItem> rows =
                userHaveItemRepository.findAllByBoothIdExcludingUser(boothId, userId);

        Set<Long> myWantItemIds = itemIdsOfWants(userId);
        // 이름 순서를 등록 순서로 고정한다. 화면이 "N Vision 74 · PONY Vision 74" 처럼 이어
        // 붙이는데, 같은 목록이 부를 때마다 순서가 달라지면 바뀐 것처럼 보인다.
        Map<Long, String> myHaveItemNames = myHaveItemNames(userId);

        // 주인들의 희망 카드는 한 번만 읽는다. 이름 목록과 id 집합 두 가지로 쓰이는데,
        // 각각 따로 읽으면 같은 쿼리가 두 번 나간다.
        List<UserWantItem> ownerWants = ownerWants(rows);
        Map<UUID, List<String>> wantNamesByOwner = wantNamesByOwner(ownerWants);
        Map<UUID, Set<Long>> wantItemIdsByOwner = wantItemIdsByOwner(ownerWants);

        List<BoothHaveItemResponseDto> sorted = rows.stream()
                .map(row -> toDto(row, myWantItemIds, myHaveItemNames,
                        wantNamesByOwner, wantItemIdsByOwner))
                .sorted(Comparator
                        .comparing(BoothHaveItemResponseDto::wanted).reversed()
                        .thenComparing(Comparator
                                .comparing(BoothHaveItemResponseDto::exchangeable).reversed())
                        .thenComparing(BoothHaveItemResponseDto::haveItemId))
                .toList();

        return PageRequestValues.slice(sorted, page, size);
    }

    private BoothHaveItemResponseDto toDto(
            UserHaveItem row,
            Set<Long> myWantItemIds,
            Map<Long, String> myHaveItemNames,
            Map<UUID, List<String>> wantNamesByOwner,
            Map<UUID, Set<Long>> wantItemIdsByOwner) {

        UUID ownerId = row.getUser().getId();
        Set<Long> ownerWants = wantItemIdsByOwner.getOrDefault(ownerId, Set.of());

        // 내가 줄 수 있는 카드 = 그 주인이 원하는 것 ∩ 내가 가진 것.
        // 이 교집합이 비어 있는 상대가 바로 "그래도 찔러보기" 대상이다.
        List<String> givableItemNames = myHaveItemNames.entrySet().stream()
                .filter(entry -> ownerWants.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        return BoothHaveItemResponseDto.of(
                row,
                myWantItemIds.contains(row.getItem().getId()),
                givableItemNames,
                wantNamesByOwner.getOrDefault(ownerId, List.of()));
    }

    private Set<Long> itemIdsOfWants(UUID userId) {
        return userWantItemRepository.findAllByUserId(userId).stream()
                .map(want -> want.getItem().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, String> myHaveItemNames(UUID userId) {
        Map<Long, String> names = new LinkedHashMap<>();
        userHaveItemRepository.findAllByUserId(userId).stream()
                .filter(have -> have.getQuantity() != null && have.getQuantity() > 0)
                .forEach(have -> names.putIfAbsent(have.getItem().getId(), have.getItem().getName()));
        return names;
    }

    private Map<UUID, List<String>> wantNamesByOwner(List<UserWantItem> ownerWants) {
        return ownerWants.stream().collect(Collectors.groupingBy(
                want -> want.getUser().getId(),
                LinkedHashMap::new,
                Collectors.mapping(want -> want.getItem().getName(), Collectors.toList())));
    }

    private Map<UUID, Set<Long>> wantItemIdsByOwner(List<UserWantItem> ownerWants) {
        return ownerWants.stream().collect(Collectors.groupingBy(
                want -> want.getUser().getId(),
                LinkedHashMap::new,
                Collectors.mapping(want -> want.getItem().getId(),
                        Collectors.toCollection(LinkedHashSet::new))));
    }

    /**
     * 목록에 뜨는 주인들의 희망 카드를 한 번에 읽는다.
     *
     * <p>{@code in ()} 은 문법 오류라, 주인이 없으면 쿼리를 아예 보내지 않는다.
     */
    private List<UserWantItem> ownerWants(List<UserHaveItem> rows) {
        Set<UUID> ownerIds = rows.stream()
                .map(row -> row.getUser().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return ownerIds.isEmpty() ? List.of() : userWantItemRepository.findAllByUserIdIn(ownerIds);
    }
}
