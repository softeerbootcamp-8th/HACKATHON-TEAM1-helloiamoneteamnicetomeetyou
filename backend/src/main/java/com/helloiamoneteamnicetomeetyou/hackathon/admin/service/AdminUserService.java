package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.HoldingView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemDetailView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemHolderView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.UserView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service.UserHaveItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service.UserWantItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseConnectionManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자와 그 사람이 들고 있는 카드, 찾는 카드를 다룬다.
 *
 * <p>부스에서 "이 카드를 가진 사람이 하나 더 있으면 매칭이 붙는데" 하는 상황을 그 자리에서
 * 해결하는 것이 이 서비스가 하는 일이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminCleanupService adminCleanupService;
    private final ItemRepository itemRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final UserHaveItemService userHaveItemService;
    private final UserWantItemService userWantItemService;
    private final ApplicationEventPublisher eventPublisher;
    private final SseConnectionManager sseConnectionManager;

    /**
     * 사용자 목록이다. 각자 들고 있는 카드까지 함께 담는다.
     *
     * <p>카드를 사람마다 따로 읽으면 사용자 수만큼 쿼리가 나간다. 전부 한 번에 읽어 와서
     * 메모리에서 묶는다. 부스 규모에서는 이쪽이 훨씬 싸다.
     */
    public List<UserView> findUsers() {
        Map<UUID, List<ItemView>> have = groupHave();
        Map<UUID, List<ItemView>> want = groupWant();
        Set<UUID> connected = sseConnectionManager.connectedUserIds();

        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> UserView.of(
                        user,
                        have.getOrDefault(user.getId(), List.of()),
                        want.getOrDefault(user.getId(), List.of()),
                        connected.contains(user.getId())))
                .toList();
    }

    /**
     * 목록에 그릴 보유 카드. <b>그 사람 화면의 내 카드와 같은 줄만 담는다.</b>
     *
     * <p>목록은 카드 얼굴만 그려서 수량과 상태가 화면에 남지 않는다. 그대로 전부 그리면 교환으로
     * 받기만 한 카드와 다 넘긴 카드까지 내놓는 카드처럼 보여서, 운영자가 보는 것과 관람객이 자기
     * 화면에서 보는 것이 갈린다. 부스에서 화면을 나란히 놓고 맞춰 볼 수 없으면 목록을 믿을 수 없다.
     *
     * <p>기준은 {@link UserHaveItem#isRegistered()} 다. 사용자 화면의 내 카드가 읽는
     * {@code findRegisteredByUserId} 와 같은 조건이라, 두 화면에 같은 카드가 뜬다.
     *
     * <p>쿼리에 조건을 넣지 않은 것은 더미 만들기가 같은 메서드로 전체 행을 읽기 때문이다.
     */
    private Map<UUID, List<ItemView>> groupHave() {
        return userHaveItemRepository.findAllWithItem().stream()
                .filter(UserHaveItem::isRegistered)
                .collect(Collectors.groupingBy(
                        row -> row.getUser().getId(),
                        Collectors.mapping(row -> ItemView.of(row.getItem()), Collectors.toList())));
    }

    private Map<UUID, List<ItemView>> groupWant() {
        return userWantItemRepository.findAllWithItem().stream()
                .collect(Collectors.groupingBy(
                        row -> row.getUser().getId(),
                        Collectors.mapping(row -> ItemView.of(row.getItem()), Collectors.toList())));
    }

    /**
     * 카드 목록. 카드마다 가진 사람과 찾는 사람을 함께 담는다.
     *
     * <p>짝이 날 수 없는 카드를 위로 올린다. 부스에서는 목록을 끝까지 훑을 시간이 없어서 손을
     * 대야 하는 것이 위에 있어야 한다.
     */
    public List<ItemDetailView> findItemDetails() {
        Set<UUID> connected = sseConnectionManager.connectedUserIds();

        return itemRepository.findAllWithBooth().stream()
                .map(item -> toItemDetail(item.getId(), ItemView.of(item), connected))
                .sorted(Comparator
                        .comparing(ItemDetailView::isDeadEnd).reversed()
                        .thenComparing(view -> view.item().name()))
                .toList();
    }

    public ItemDetailView findItemDetail(Long itemId) {
        Item item = findItem(itemId);
        return toItemDetail(itemId, ItemView.of(item), sseConnectionManager.connectedUserIds());
    }

    private ItemDetailView toItemDetail(Long itemId, ItemView item, Set<UUID> connected) {
        List<ItemHolderView> holders = userHaveItemRepository.findAllByItemId(itemId).stream()
                .map(have -> ItemHolderView.of(have, connected))
                .toList();

        List<UserView> seekers = userWantItemRepository.findAllByItemId(itemId).stream()
                .map(want -> UserView.of(want.getUser(), List.of(), List.of(), connected.contains(want.getUser().getId())))
                .toList();

        return new ItemDetailView(item, holders, seekers);
    }

    public User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 세부 화면의 내놓는 카드. <b>사용자 화면의 내 카드가 읽는 쿼리를 그대로 쓴다.</b>
     *
     * <p>{@code /api/have-items} 가 부르는 {@code findRegisteredByUserId} 라서, 운영자가 보는
     * 줄과 그 사람이 자기 화면에서 보는 줄이 같다. 전부를 읽던 때에는 다 넘긴 카드와 교환으로
     * 받기만 한 카드가 {@code 다 나감} 배지를 달고 남아서, 관람객 화면에는 없는 카드를 운영자만
     * 보고 있었다.
     *
     * <p>숨긴 줄을 다시 내놓는 길은 막히지 않는다. 위쪽 추가 폼이 부르는
     * {@code UserHaveItemService.register} 가 이미 있는 행이면 개수를 그 값으로 덮어써서,
     * 다 넘긴 카드도 같은 개수로 다시 등록된다.
     */
    public List<HoldingView> findHaveItems(UUID userId) {
        return userHaveItemRepository.findRegisteredByUserId(userId).stream()
                .map(HoldingView::of)
                .toList();
    }

    public List<HoldingView> findWantItems(UUID userId) {
        return userWantItemRepository.findAllByUserId(userId).stream()
                .map(HoldingView::of)
                .toList();
    }

    /**
     * 더미 사용자를 만든다. UUID 는 서버가 발급한다.
     *
     * <p>화면에서 등록하는 사용자는 클라이언트가 UUID 를 만들어 보내는데, 어드민에는 그 UUID 를
     * 들고 있을 기기가 없다. 여기서 만들어 두면 부스 운영자가 그 값을 볼 일도 없다.
     */
    @Transactional
    public UUID createDummy(String username) {
        return userRepository.save(User.dummy(UUID.randomUUID(), username)).getId();
    }

    /**
     * 카드까지 정해서 사용자 하나를 만든다.
     *
     * <p>더미를 한 번에 만드는 쪽은 카드를 고리로 알아서 배분하는데, 부스에서는 "이 카드를 가진
     * 사람이 지금 하나 더 필요하다" 가 되는 경우가 더 많다. 그때 만들고 나서 카드를 따로 붙이면
     * 손이 두 번 가고, 그 사이에 관람객이 기다린다.
     *
     * <p>수량은 고른 카드와 같은 순서로 온다. 화면이 고르지 않은 타일의 수량 칸을 꺼서 보내기
     * 때문에 두 목록의 길이가 같다. 그래도 어긋난 채로 오면 그 자리는 1 로 본다. 수량이 하나
     * 틀린 것보다 사용자가 아예 안 만들어지는 쪽이 부스에서 더 곤란하다.
     *
     * @param haveItemIds 내놓는 카드. 비어 있어도 된다
     * @param haveQuantities 내놓는 카드의 장수. 비어 있으면 전부 1 장이다
     * @param wantItemIds 찾는 카드. 비어 있어도 된다
     * @param wantQuantities 찾는 카드의 장수. 비어 있으면 전부 1 장이다
     */
    @Transactional
    public UUID createDummy(
            String username,
            List<Long> haveItemIds,
            List<Integer> haveQuantities,
            List<Long> wantItemIds,
            List<Integer> wantQuantities) {

        UUID userId = createDummy(username);

        forEachPicked(haveItemIds, haveQuantities, (itemId, quantity) -> addHaveItem(userId, itemId, quantity));
        forEachPicked(wantItemIds, wantQuantities, (itemId, quantity) -> addWantItem(userId, itemId, quantity));

        return userId;
    }

    /** 고른 카드를 하나씩 훑는다. 짝이 없거나 1 보다 작은 수량은 1 로 본다. */
    private void forEachPicked(List<Long> itemIds, List<Integer> quantities, BiConsumer<Long, Integer> action) {
        if (itemIds == null) {
            return;
        }

        for (int i = 0; i < itemIds.size(); i++) {
            Long itemId = itemIds.get(i);
            if (itemId == null) {
                continue;
            }

            Integer quantity = quantities != null && i < quantities.size() ? quantities.get(i) : null;
            action.accept(itemId, quantity == null || quantity < 1 ? 1 : quantity);
        }
    }

    @Transactional
    public void rename(UUID userId, String username) {
        findUser(userId).rename(username);
    }

    /**
     * 보유 카드를 붙인다. 이미 있으면 수량만 더한다.
     *
     * <p>같은 카드를 두 줄로 두면 매칭이 그 사람을 두 번 세게 된다. 부스에서 실수로 두 번 누르는
     * 일이 생길 자리라 여기서 막는다.
     */
    /**
     * 내놓는 카드를 붙인다.
     *
     * <p><b>사용자 화면과 같은 서비스를 부른다.</b> 예전에는 리포지토리에 직접 썼는데, 그러면
     * {@code MatchTriggerEvent} 가 안 나가서 매칭이 돌지 않았다. 어드민으로 더미에게 딱 맞는
     * 카드를 붙여 놓고도 상대 화면에 매칭이 안 뜨는 것이 여기서 생긴 일이다.
     *
     * <p>부스 목록을 갱신하는 {@code USER_JOINED} 도 그 서비스가 함께 내보낸다.
     */
    @Transactional
    public void addHaveItem(UUID userId, Long itemId, int quantity) {
        userHaveItemService.register(userId, itemId, quantity);
    }

    @Transactional
    public void changeHaveQuantity(Long haveId, int quantity) {
        UserHaveItem have = userHaveItemRepository.findById(haveId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        have.changeQuantity(quantity);

        // 0개였던 카드가 1개가 되면 그 순간부터 매칭 후보가 된다. 다시 돌려 준다.
        eventPublisher.publishEvent(new MatchTriggerEvent(have.getUser().getId()));
    }

    /**
     * 카드를 뗀다.
     *
     * <p>떼고 나서도 매칭을 다시 돌린다. 이 사람이 물고 있던 조합이 사라지면서 다른 조합이
     * 열릴 수 있고, 무엇보다 어드민에서 카드를 갈아 끼우는 것이 "떼고 붙이기" 라서 뗀 직후
     * 상태가 그대로 굳으면 붙이기 전까지 매칭이 멈춘 것처럼 보인다.
     */
    @Transactional
    public void removeHaveItem(Long haveId) {
        userHaveItemRepository.findById(haveId)
                .map(have -> have.getUser().getId())
                .ifPresent(userId -> {
                    userHaveItemRepository.deleteById(haveId);
                    eventPublisher.publishEvent(new MatchTriggerEvent(userId));
                });
    }

    /**
     * 찾는 카드를 붙인다. 이미 있으면 개수만 덮어쓴다.
     *
     * <p>{@link #addHaveItem} 과 같은 이유로 사용자 화면과 같은 서비스를 부른다.
     */
    @Transactional
    public void addWantItem(UUID userId, Long itemId, int quantity) {
        userWantItemService.register(userId, itemId, quantity);
    }

    /** 카드 화면처럼 수량을 고를 자리가 없는 곳에서 부른다. */
    @Transactional
    public void addWantItem(UUID userId, Long itemId) {
        addWantItem(userId, itemId, 1);
    }

    @Transactional
    public void removeWantItem(Long wantId) {
        userWantItemRepository.findById(wantId)
                .map(want -> want.getUser().getId())
                .ifPresent(userId -> {
                    userWantItemRepository.deleteById(wantId);
                    eventPublisher.publishEvent(new MatchTriggerEvent(userId));
                });
    }

    /**
     * 사용자를 지운다. 그 사람이 남긴 카드와 교환, 찔러보기, 알림도 같이 걷어낸다.
     *
     * <p><b>더미가 아닌 실제 참가자도 지운다.</b> 예전에는 더미만 열어 뒀는데, 부스에서 잘못
     * 등록된 사람이나 시연이 끝난 관람객을 치울 방법이 없어서 운영자가 DB 를 직접 열어야 했다.
     * 지워진 사람의 화면은 그때부터 아무것도 못 하게 되므로, 무엇을 지우는지는 화면이 먼저
     * 확인받는다.
     *
     * <p>등록한 카드만 지우고 있었더니 찔러보기와 교환, 알림, 푸시 구독에 걸려 500 이 나갔다.
     * 사람을 붙들고 있는 표가 여덟이라, 지우는 순서를 아는 곳을 {@link AdminCleanupService}
     * 하나로 모아 두고 여기서는 그것만 부른다.
     *
     * @return 같이 지운 교환 건수
     */
    @Transactional
    public int deleteUser(UUID userId) {
        User user = findUser(userId);

        int removedExchanges = adminCleanupService.deleteUserDeep(userId);
        userRepository.delete(user);

        return removedExchanges;
    }

    private Item findItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
