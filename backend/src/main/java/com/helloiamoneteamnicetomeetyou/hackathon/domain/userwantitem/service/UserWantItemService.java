package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
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

    /**
     * 희망 카드를 등록한다. 이미 등록한 카드면 원하는 개수를 덮어쓴다.
     *
     * <p>(userId, itemId) 로 묶어 한 행만 유지한다. 등록 화면(`NeedsSelect`)이 아이템마다 수량
     * 하나를 들고 +/- 로 조절하는 형태라, 요청은 "이번에 몇 개 더" 가 아니라 "지금 몇 개다" 를
     * 보낸다. 행을 쌓으면 그 화면의 수량 감소와 선택 취소를 서버에 반영할 방법이 없다.
     *
     * <p>보유 카드 등록도 같은 화면을 쓰므로 {@code UserHaveItemService.register} 와 동작이 같다.
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

        Optional<UserWantItem> existing = userWantItemRepository.findByUserIdAndItemId(userId, itemId);
        if (existing.isPresent()) {
            existing.get().changeQuantity(quantity);
            return false;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ITEM_NOT_FOUND));

        userWantItemRepository.save(UserWantItem.of(user, item, quantity));

        return true;
    }
}
