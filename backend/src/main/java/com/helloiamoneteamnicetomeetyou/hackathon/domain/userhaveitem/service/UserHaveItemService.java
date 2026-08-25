package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserHaveItemService {

    private final UserHaveItemRepository userHaveItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    /**
     * 보유 카드를 등록한다. 등록할 때마다 새 행을 만든다.
     *
     * <p>부스 관리자가 올린 상품 목록에서 여러 종류를 고르고 각 상품 개수를 여러 번에 나눠 등록할
     * 수 있는 흐름이라, 같은 (userId, itemId) 조합이 다시 들어와도 기존 행을 찾아 합치지 않는다.
     * 그래서 이 메서드는 언제나 새로 만든다 — PR #22 의 {@code UserService.register} 와 달리
     * 멱등이 아니다.
     */
    @Transactional
    public void register(UUID userId, Long itemId, Integer quantity) {
        // Bean Validation 이 아직 없어서 형식 검증을 여기서 한다.
        if (userId == null || itemId == null || quantity == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }
        if (quantity < 1) {
            throw new ApplicationException(ErrorCode.INVALID_QUANTITY);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ITEM_NOT_FOUND));

        userHaveItemRepository.save(UserHaveItem.of(user, item, quantity));
    }
}
