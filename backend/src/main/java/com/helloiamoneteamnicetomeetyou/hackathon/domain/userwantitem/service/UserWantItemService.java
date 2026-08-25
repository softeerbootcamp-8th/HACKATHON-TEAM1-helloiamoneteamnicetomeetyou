package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
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
     * 희망 카드를 등록한다. 이미 있으면 아무것도 하지 않는다.
     *
     * <p>수량 개념이 없어 같은 카드를 다시 등록하는 것이 의미를 갖지 않으므로, 보유 카드 등록과
     * 달리 PR #22 의 {@code UserService.register} 와 같은 멱등 방식을 쓴다.
     *
     * @return 이번 호출로 새로 만들었으면 true
     */
    @Transactional
    public boolean register(UUID userId, Long itemId) {
        // Bean Validation 이 아직 없어서 형식 검증을 여기서 한다.
        if (userId == null || itemId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ITEM_NOT_FOUND));

        if (userWantItemRepository.existsByUserIdAndItemId(userId, itemId)) {
            return false;
        }

        userWantItemRepository.save(UserWantItem.of(user, item));

        return true;
    }
}
