package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.PokeView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.repository.PokeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.service.PokeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 찔러보기를 어드민에서 다룬다.
 *
 * <p>실기기 한 대로 시연할 때 상대 쪽을 눌러 줄 사람이 없어서 찔러보기 흐름이 통째로 막힌다.
 * 더미가 보내는 것과 더미가 답하는 것 둘 다 여기서 대신 눌러 준다.
 *
 * <p><b>대리 조작은 더미에게만 연다.</b> 실제 사용자에게 온 찔러보기를 운영자가 대신 수락하면
 * 그 사람이 하지 않은 교환이 그 사람 이름으로 성사된다.
 *
 * <p>검증과 SSE 는 {@link PokeService} 가 전부 한다. 여기서 다시 만들지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPokeService {

    private final PokeRepository pokeRepository;
    private final PokeService pokeService;
    private final UserRepository userRepository;
    private final UserHaveItemRepository userHaveItemRepository;

    /**
     * 찔러보기 목록. 답을 기다리는 건에는 보낸 사람이 내놓은 카드를 함께 담는다.
     *
     * <p>수락하려면 그중 한 장을 골라야 해서, 목록이 없으면 운영자가 카드 전체에서 찍어
     * 맞혀야 한다. 이미 답이 끝난 건은 고를 일이 없으므로 읽지 않는다.
     */
    public List<PokeView> findPokes() {
        return pokeRepository.findAllForAdmin().stream()
                .map(poke -> PokeView.of(poke, poke.isPending() ? offerableItems(poke) : List.of()))
                .toList();
    }

    /** 보낸 사람이 지금 내놓고 있는 카드. 수량이 0 인 것은 이미 다 나간 카드라 뺀다. */
    private List<ItemView> offerableItems(Poke poke) {
        return userHaveItemRepository.findAllByUserId(poke.getFromUser().getId()).stream()
                .filter(have -> have.getQuantityLeft() != null && have.getQuantityLeft() > 0)
                .map(UserHaveItem::getItem)
                .map(ItemView::of)
                .toList();
    }

    /**
     * 더미가 실제 사용자를 찔러본다.
     *
     * <p>{@code requestedItemId} 는 <b>상대가 내놓고 있는</b> 카드다. 서비스가 상대 보유 여부와
     * 수량, 중복 여부까지 확인하고 상대에게 {@code POKE_RECEIVED} 를 보낸다.
     */
    @Transactional
    public void sendAsDummy(UUID fromUserId, UUID toUserId, Long requestedItemId) {
        requireDummy(fromUserId);

        pokeService.send(fromUserId, toUserId, requestedItemId);
    }

    /**
     * 더미가 받은 찔러보기에 답한다.
     *
     * <p>수락이면 {@code chosenItemId} 가 있어야 한다. 보낸 사람이 내놓은 카드 중 더미가 받을
     * 한 장이고, 그 자리에서 교환까지 만들어진다.
     */
    @Transactional
    public void answerAsDummy(Long pokeId, PokeStatus status, Long chosenItemId) {
        Poke poke = pokeRepository.findByIdWithUsers(pokeId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.POKE_NOT_FOUND));

        UUID receiverId = poke.getToUser().getId();
        requireDummy(receiverId);

        pokeService.answer(pokeId, receiverId, status, chosenItemId);
    }

    /** 실제 사용자를 대신해 누르지 못하게 막는 유일한 문이다. */
    private void requireDummy(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        if (!user.isAdminManaged()) {
            throw new ApplicationException(ErrorCode.POKE_NOT_RECEIVER);
        }
    }
}
