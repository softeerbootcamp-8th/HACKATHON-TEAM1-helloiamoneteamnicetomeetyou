package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.BoothRosterChangedDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.dto.WantItemRegisteredResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWantItemService {

    private final UserWantItemRepository userWantItemRepository;
    // 상호 배제 검증에만 쓴다. 서비스가 아니라 레포지토리를 받는 것은 두 서비스가 서로를
    // 주입하면 순환 의존이 되기 때문이다.
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SseEventPublisher sseEventPublisher;

    /**
     * 찾는 카드를 등록한다. 이미 등록한 카드면 개수를 덮어쓴다.
     *
     * <p>(userId, itemId) 로 묶어 한 행만 유지한다. 등록 화면이 카드마다 개수 하나를 들고 +/- 로
     * 조절하는 형태라 요청이 보내는 것은 "이번에 몇 개 더" 가 아니라 "지금 몇 개다" 다. 행을
     * 쌓으면 그 화면의 개수 감소와 선택 취소를 반영할 방법이 없고, 교환이 성사돼 찾는 수량이 줄
     * 때도 어느 행에서 뺄지 정할 수 없다.
     *
     * <p>내놓을 카드도 같은 화면을 쓰므로 {@code UserHaveItemService.register} 와 동작이 같다.
     *
     * @return 이번 호출로 새로 만들었으면 true, 기존 값을 덮어썼으면 false
     */
    @Transactional
    public boolean register(UUID userId, Long itemId, Integer quantity) {
        // Bean Validation 이 아직 없어서 형식 검증을 여기서 한다.
        if (userId == null || itemId == null || quantity == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }
        if (quantity < 1) {
            throw new ApplicationException(ErrorCode.INVALID_QUANTITY);
        }
        // 내놓기로 한 카드를 동시에 찾을 수는 없다. 화면이 그 카드를 비활성으로 막고 있지만,
        // 화면 제어만으로는 보장되지 않는다. 두 목록에 같은 카드가 들어가면 매칭이 그 카드를
        // 주면서 동시에 받는 교환을 만든다.
        if (userHaveItemRepository.existsByUserIdAndItemIdAndQuantityLeftGreaterThan(userId, itemId, 0)) {
            throw new ApplicationException(ErrorCode.ITEM_ALREADY_IN_HAVE);
        }

        Optional<UserWantItem> existing =
                userWantItemRepository.findByUserIdAndItemId(userId, itemId);
        if (existing.isPresent()) {
            // 개수만 바뀐 것이라 목록에 없던 카드가 새로 생기지는 않는다. 알리지 않는다.
            existing.get().changeQuantity(quantity);
            eventPublisher.publishEvent(new MatchTriggerEvent(userId));
            return false;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ITEM_NOT_FOUND));

        userWantItemRepository.save(UserWantItem.of(user, item, quantity));
        eventPublisher.publishEvent(new MatchTriggerEvent(userId));

        // 같은 부스를 보고 있는 사람들의 목록이 이 등록으로 달라진다. 알리지 않으면 상대가
        // 새로고침할 때까지 레이더에 뜨지 않는다.
        //
        // 부스 전체로 가는 이벤트라 웹푸시로는 나가지 않는다. PushEventDispatcher 가 사용자
        // 지정 이벤트만 푸시한다. 등록 한 번에 전원의 잠금 화면이 울리면 스팸이다.
        sseEventPublisher.toBooth(
                item.getBooth().getId(),
                SseEventType.USER_JOINED,
                new BoothRosterChangedDto(item.getBooth().getId(), item.getId()));

        return true;
    }

    /**
     * 내가 지금 등록해 둔 찾는 카드 전부.
     *
     * <p>등록 화면이 제출 직전에 읽는다. 화면 상태는 새로고침에 사라지므로, 무엇을 해제해야
     * 하는지는 서버가 들고 있는 이 목록과 견줘야만 알 수 있다.
     */
    public List<WantItemRegisteredResponseDto> findMine(UUID userId) {
        if (userId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        return userWantItemRepository.findAllByUserId(userId).stream()
                .map(WantItemRegisteredResponseDto::from)
                .toList();
    }

    /**
     * 찾는 카드 등록을 해제한다.
     *
     * <p><b>없는 줄이면 아무 일도 하지 않고 끝낸다.</b> 화면이 재시도하거나 이미 지운 카드를 다시
     * 지우려 해도 깨지지 않아야 한다. 지우는 것이 목적이지 "지울 것이 있었다" 가 목적이 아니다.
     *
     * <p>재매칭을 걸지 않는다. 찾는 카드가 줄어드는 방향이라 이걸로 새 매칭이 생길 수는 없다.
     */
    @Transactional
    public void remove(UUID userId, Long itemId) {
        if (userId == null || itemId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        userWantItemRepository.findByUserIdAndItemId(userId, itemId)
                .ifPresent(userWantItemRepository::delete);
    }
}
