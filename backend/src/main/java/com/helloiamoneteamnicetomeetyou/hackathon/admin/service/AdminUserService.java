package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.HoldingView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemDetailView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.UserView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseConnectionManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
    private final SseConnectionManager sseConnectionManager;

    /**
     * 사용자 목록이다. 카드 수는 사람마다 따로 세지 않고 한 번에 묶어서 읽는다.
     *
     * <p>사람마다 세면 사용자 수 곱하기 2 만큼 쿼리가 나간다. 부스 이틀이면 사람이 계속 쌓이는
     * 화면이라 처음부터 묶어 두는 편이 낫다.
     */
    public List<UserView> findUsers() {
        Map<UUID, Long> haveCounts = toCountMap(userHaveItemRepository.countByUser());
        Map<UUID, Long> wantCounts = toCountMap(userWantItemRepository.countByUser());
        Set<UUID> connected = sseConnectionManager.connectedUserIds();

        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> UserView.of(
                        user,
                        haveCounts.getOrDefault(user.getId(), 0L).intValue(),
                        wantCounts.getOrDefault(user.getId(), 0L).intValue(),
                        connected.contains(user.getId())))
                .toList();
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    public long countUsers() {
        return userRepository.count();
    }

    /** 어드민이 세워 둔 더미 수. 시연 판이 차려져 있는지를 이 숫자로 본다. */
    public long countDummies() {
        return userRepository.findAll().stream().filter(User::isAdminManaged).count();
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
        List<UserView> holders = userHaveItemRepository.findAllByItemId(itemId).stream()
                .map(have -> UserView.of(have.getUser(), 0, 0, connected.contains(have.getUser().getId())))
                .toList();

        List<UserView> seekers = userWantItemRepository.findAllByItemId(itemId).stream()
                .map(want -> UserView.of(want.getUser(), 0, 0, connected.contains(want.getUser().getId())))
                .toList();

        return new ItemDetailView(item, holders, seekers);
    }

    public User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
    }

    public List<HoldingView> findHaveItems(UUID userId) {
        return userHaveItemRepository.findAllByUserId(userId).stream()
                .map(have -> new HoldingView(have.getId(), ItemView.of(have.getItem()), have.getQuantity()))
                .toList();
    }

    public List<HoldingView> findWantItems(UUID userId) {
        return userWantItemRepository.findAllByUserId(userId).stream()
                .map(want -> new HoldingView(want.getId(), ItemView.of(want.getItem()), null))
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
    @Transactional
    public void addHaveItem(UUID userId, Long itemId, int quantity) {
        userHaveItemRepository.findByUserIdAndItemId(userId, itemId).ifPresentOrElse(
                have -> have.changeQuantity(have.getQuantity() + quantity),
                () -> userHaveItemRepository.save(UserHaveItem.of(findUser(userId), findItem(itemId), quantity)));
    }

    @Transactional
    public void changeHaveQuantity(Long haveId, int quantity) {
        userHaveItemRepository.findById(haveId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND))
                .changeQuantity(quantity);
    }

    @Transactional
    public void removeHaveItem(Long haveId) {
        userHaveItemRepository.deleteById(haveId);
    }

    /** 희망 카드를 붙인다. 수량 개념이 없어서 이미 있으면 아무것도 하지 않는다. */
    @Transactional
    public void addWantItem(UUID userId, Long itemId) {
        if (userWantItemRepository.existsByUserIdAndItemId(userId, itemId)) {
            return;
        }
        userWantItemRepository.save(UserWantItem.of(findUser(userId), findItem(itemId)));
    }

    @Transactional
    public void removeWantItem(Long wantId) {
        userWantItemRepository.deleteById(wantId);
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
