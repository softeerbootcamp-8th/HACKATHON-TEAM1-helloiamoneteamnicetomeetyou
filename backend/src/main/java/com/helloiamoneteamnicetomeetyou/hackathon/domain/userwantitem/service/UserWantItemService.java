package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.BoothRosterChangedDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWantItemService {

    private final UserWantItemRepository userWantItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
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

        Optional<UserWantItem> existing =
                userWantItemRepository.findByUserIdAndItemId(userId, itemId);
        if (existing.isPresent()) {
            // 개수만 바뀐 것이라 목록에 없던 카드가 새로 생기지는 않는다. 알리지 않는다.
            existing.get().changeQuantity(quantity);
            return false;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ITEM_NOT_FOUND));

        userWantItemRepository.save(UserWantItem.of(user, item, quantity));

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
}
