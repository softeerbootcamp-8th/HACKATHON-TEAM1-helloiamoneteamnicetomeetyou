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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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

    private Map<UUID, List<ItemView>> groupHave() {
        return userHaveItemRepository.findAllWithItem().stream()
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

        return itemRepository.findAll().stream()
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

    public List<HoldingView> findHaveItems(UUID userId) {
        return userHaveItemRepository.findAllByUserId(userId).stream()
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
     * @param haveItemIds 내놓는 카드. 비어 있어도 된다
     * @param wantItemIds 찾는 카드. 비어 있어도 된다
     */
    @Transactional
    public UUID createDummy(String username, List<Long> haveItemIds, List<Long> wantItemIds) {
        UUID userId = createDummy(username);

        if (haveItemIds != null) {
            haveItemIds.stream().filter(Objects::nonNull).forEach(itemId -> addHaveItem(userId, itemId, 1));
        }
        if (wantItemIds != null) {
            wantItemIds.stream().filter(Objects::nonNull).forEach(itemId -> addWantItem(userId, itemId));
        }

        return userId;
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
     * <p>{@link #addHaveItem} 과 같은 이유로 사용자 화면과 같은 서비스를 부른다. 어드민에는
     * 찾는 개수를 넣는 자리가 없어서 1 로 만든다.
     */
    @Transactional
    public void addWantItem(UUID userId, Long itemId) {
        userWantItemService.register(userId, itemId, 1);
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
     * 더미 사용자를 지운다. 그 사람이 들고 있던 카드도 같이 지운다.
     *
     * <p><b>진짜 참가자는 지우지 않는다.</b> 부스에서 실제로 앱을 쓰고 있는 사람을 목록에서
     * 지우면 그 사람 화면이 그때부터 아무것도 못 하게 된다. 어드민이 만든 더미만 열어 둔다.
     */
    @Transactional
    public void deleteDummy(UUID userId) {
        User user = findUser(userId);
        if (!user.isAdminManaged()) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        userHaveItemRepository.deleteByUserId(userId);
        userWantItemRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private Item findItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
