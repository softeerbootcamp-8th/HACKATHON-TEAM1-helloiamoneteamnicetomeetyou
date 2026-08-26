package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeLock;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeAnswerResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeEventDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeSendResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.ReceivedPokeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.SentPokeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.repository.PokeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageRequestValues;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 찔러보기. 서로 원하는 것이 맞지 않는 상대에게 보내는 단방향 제안이다.
 *
 * <p>보내는 사람은 받고 싶은 카드 한 장만 지정한다. 내줄 카드는 <b>받는 쪽이</b> 보낸 사람의
 * 보유 묶음에서 고른다. 그래서 수락이 곧 성사고, 보낸 사람에게 다시 묻지 않는다
 * (시안 desc 165:3683).
 *
 * <p>알림은 {@code sseEventPublisher.toUser(...)} 하나만 부른다. 상대가 앱을 보고 있으면 SSE 로,
 * 닫아 뒀으면 웹푸시로 나가는 것은 {@code PushEventDispatcher} 가 알아서 갈라 준다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PokeService {

    private final PokeRepository pokeRepository;
    private final UserRepository userRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final ExchangeService exchangeService;
    private final ExchangeLock exchangeLock;
    private final SseEventPublisher sseEventPublisher;

    /**
     * 찔러보기를 보낸다.
     *
     * <p>보내는 사람의 보유 카드가 하나도 없으면 막는다. 받는 쪽 화면이 "상대의 카드 묶음에서
     * 한 장을 고르세요" 인데 고를 것이 없으면 그 화면이 성립하지 않는다.
     *
     * <p><b>같은 상대에게 답을 기다리는 중이면 막는다.</b> 총 횟수에는 제한이 없다
     * (시안 desc 165:3613, 165:3514).
     */
    @Transactional
    public PokeSendResponseDto send(UUID userId, UUID targetUserId, Long requestedItemId) {
        // Bean Validation 이 아직 없어서 형식 검증을 여기서 한다.
        if (userId == null || targetUserId == null || requestedItemId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }
        if (userId.equals(targetUserId)) {
            throw new ApplicationException(ErrorCode.POKE_SELF_NOT_ALLOWED);
        }

        User fromUser = findUser(userId);
        User toUser = findUser(targetUserId);

        // 상대가 실제로 그 카드를 지금 내놓고 있는지. quantityLeft 가 0 이면 전부 나갔거나
        // 다른 교환에 예약돼 있어서, 총 등록 수량(quantity)만으로는 알 수 없다.
        UserHaveItem targetHave = userHaveItemRepository
                .findByUserIdAndItemId(targetUserId, requestedItemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.POKE_ITEM_NOT_OWNED));
        if (targetHave.getQuantityLeft() == null || targetHave.getQuantityLeft() < 1) {
            throw new ApplicationException(ErrorCode.POKE_ITEM_SOLD_OUT);
        }

        if (offerableItems(userId).isEmpty()) {
            throw new ApplicationException(ErrorCode.POKE_NO_OFFERABLE_ITEM);
        }
        if (pokeRepository.existsByFromUserIdAndToUserIdAndStatus(
                userId, targetUserId, PokeStatus.PENDING)) {
            throw new ApplicationException(ErrorCode.POKE_DUPLICATE_PENDING);
        }

        Poke poke = pokeRepository.save(Poke.of(fromUser, toUser, targetHave.getItem()));

        sseEventPublisher.toUser(targetUserId, SseEventType.POKE_RECEIVED, PokeEventDto.from(poke));

        return PokeSendResponseDto.from(poke);
    }

    /**
     * 받은 찔러보기 목록이다. 답을 기다리는 것만 준다.
     *
     * <p>내놓은 묶음은 저장해 둔 값이 아니라 <b>보낸 사람의 현재 보유 카드</b>다. 제안한 뒤
     * 그 사람이 다른 교환으로 카드를 내보냈으면 여기서 자동으로 빠진다.
     */
    public PageResponse<ReceivedPokeResponseDto> findReceived(UUID userId, int page, int size) {
        if (userId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        List<ReceivedPokeResponseDto> received =
                pokeRepository.findAllByToUserIdAndStatus(userId, PokeStatus.PENDING).stream()
                        .map(poke -> ReceivedPokeResponseDto.of(
                                poke, offerableItems(poke.getFromUser().getId())))
                        .toList();

        return PageRequestValues.slice(received, page, size);
    }

    /**
     * 내가 보낸 찔러보기 목록이다. 거절되고 수락된 것까지 전부 준다.
     *
     * <p>대기 중인 상대의 카드를 화면에서 비활성화하는 데 쓰고, 알림을 놓친 뒤 다시 붙었을 때
     * 상태를 되살리는 데도 쓴다. 그래서 {@code PENDING} 만 걸러 주지 않는다.
     */
    public PageResponse<SentPokeResponseDto> findSent(UUID userId, int page, int size) {
        if (userId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        List<SentPokeResponseDto> sent = pokeRepository.findAllByFromUserId(userId).stream()
                .map(SentPokeResponseDto::from)
                .toList();

        return PageRequestValues.slice(sent, page, size);
    }

    /**
     * 받은 찔러보기에 답한다. 수락과 거절이 한 자리다.
     *
     * <p><b>{@code READ_COMMITTED} 인 이유.</b> 수락이 {@link ExchangeLock} 으로 두 사람을 잠그고
     * 이미 묶여 있는지 다시 보는데, MySQL 기본값인 REPEATABLE READ 에서는 그 재확인이 이
     * 트랜잭션의 첫 읽기 때 뜬 스냅샷을 본다. 잠금을 걸고 기다린 뒤에도 그 사이 자동 매칭이
     * 만든 참가자 줄이 보이지 않아 그대로 통과한다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PokeAnswerResponseDto answer(
            Long pokeId, UUID userId, PokeStatus status, Long chosenItemId) {

        if (pokeId == null || userId == null || status == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }
        if (status != PokeStatus.ACCEPTED && status != PokeStatus.REJECTED) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        Poke poke = pokeRepository.findByIdWithUsers(pokeId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.POKE_NOT_FOUND));

        // 받은 사람만 답할 수 있다. 남의 찔러보기를 수락하면 그 사람의 카드가 오간다.
        if (!poke.getToUser().getId().equals(userId)) {
            throw new ApplicationException(ErrorCode.POKE_NOT_RECEIVER);
        }
        if (!poke.isPending()) {
            throw new ApplicationException(ErrorCode.POKE_ALREADY_ANSWERED);
        }

        if (status == PokeStatus.REJECTED) {
            poke.reject();
            publishToSender(poke, SseEventType.POKE_REJECTED);
            return PokeAnswerResponseDto.from(poke);
        }

        accept(poke, chosenItemId);
        publishToSender(poke, SseEventType.POKE_ACCEPTED);

        return PokeAnswerResponseDto.from(poke);
    }

    /**
     * 고른 카드로 교환을 만든다.
     *
     * <p>카드는 두 줄이 생긴다. 보낸 사람이 고른 카드를 내주고, 받은 사람이 요청받은 카드를
     * 내준다. <b>방향을 뒤집으면 화면은 그럴듯한데 기록이 반대가 된다.</b>
     *
     * <p>잔여 수량을 미리 잡아 두지 않기 때문에 여기서 다시 확인한다. 제안을 보낸 뒤 그 카드가
     * 다른 교환으로 나갔을 수 있다.
     *
     * <p><b>두 사람이 이미 다른 교환에 묶여 있으면 만들지 않는다.</b> 대기장은 상대가 자동
     * 매칭으로 짝이 잡힌 뒤에도 카드를 계속 보여 주기 때문에, 이미 매칭된 두 사람이 서로를
     * 찔러보고 수락하는 일이 실제로 일어난다. 그대로 두면 같은 두 사람에게 교환이 두 건 잡혀서
     * 약속 화면과 식별 화면이 어느 쪽을 가리키는지 알 수 없게 된다.
     *
     * <p>찔러보기는 {@code PENDING} 으로 남겨 둔다. 여기서 거절로 바꾸면 세 후보 쿼리의 거절
     * 이력 필터가 그 카드 조합을 영구히 제외해서, 지금 교환이 끝난 뒤에도 두 사람이 그 카드로는
     * 다시 매칭되지 않는다.
     */
    private void accept(Poke poke, Long chosenItemId) {
        if (chosenItemId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        User sender = poke.getFromUser();
        User receiver = poke.getToUser();

        UserHaveItem senderHave = offerableItems(sender.getId()).stream()
                .filter(have -> have.getItem().getId().equals(chosenItemId))
                .findFirst()
                .orElseThrow(
                        () -> new ApplicationException(ErrorCode.POKE_CHOSEN_ITEM_NOT_OFFERED));
        Item chosenItem = senderHave.getItem();

        UserHaveItem receiverHave = userHaveItemRepository
                .findByUserIdAndItemId(receiver.getId(), poke.getRequestedItem().getId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.POKE_ITEM_SOLD_OUT));
        if (receiverHave.getQuantityLeft() == null || receiverHave.getQuantityLeft() < 1) {
            throw new ApplicationException(ErrorCode.POKE_ITEM_SOLD_OUT);
        }

        if (!exchangeLock.acquire(List.of(sender.getId(), receiver.getId()))) {
            throw new ApplicationException(ErrorCode.POKE_ALREADY_MATCHED);
        }

        /*
          교환을 만드는 길은 ExchangeService 하나다. 만나는 자리와 시간 격자 시작점, 약속별
          식별자가 거기서 함께 붙는다. 여기서 Exchange 를 직접 만들면 그것들이 비어서 약속
          화면이 장소도 시간표도 못 그린다.

          참가자도 그쪽이 넣는다. 찔러보기로 온 교환은 두 사람의 뜻이 이미 확인된 상태라
          수락 상태로 들어간다.
        */
        Long boothId = poke.getRequestedItem().getBooth().getId();
        Exchange exchange = exchangeService.createExchange(
                boothId, ExchangeType.ONE_TO_ONE, List.of(sender.getId(), receiver.getId()));

        exchangeItemRepository.saveAll(List.of(
                ExchangeItem.of(exchange, sender, chosenItem, receiver),
                ExchangeItem.of(exchange, receiver, poke.getRequestedItem(), sender)));

        // 찔러보기 수락은 카드 한 장씩만 오간다 (ExchangeItem.of 가 수량을 1로 고정하는 것과
        // 같다). 이걸 안 하면 완료 전까지 이 카드가 계속 다른 사람에게도 제안 가능한 것으로
        // 남아서, 같은 카드를 두 사람에게 동시에 약속하는 일이 생긴다.
        senderHave.reserve(1);
        receiverHave.reserve(1);

        poke.accept(chosenItem, exchange);
    }

    /**
     * 내놓을 수 있는 카드. 지금 새로 내줄 수 있는 개수(quantityLeft)가 남은 것만이다.
     *
     * <p>{@code quantity}(총 등록 수량)로 걸러내면 이미 다 나갔거나 다른 교환에 예약된 카드도
     * "내놓을 수 있다" 고 나온다. 그 카드는 총 등록량이 여전히 양수이기 때문이다.
     *
     * <p>{@code (user, item)} 한 행만 유지되므로 같은 카드가 두 번 나오지 않는다
     * ({@code UserHaveItemService.register} 가 개수를 덮어쓴다).
     */
    private List<UserHaveItem> offerableItems(UUID userId) {
        return userHaveItemRepository.findAllByUserId(userId).stream()
                .filter(have -> have.getQuantityLeft() != null && have.getQuantityLeft() > 0)
                .toList();
    }

    private void publishToSender(Poke poke, SseEventType type) {
        sseEventPublisher.toUser(poke.getFromUser().getId(), type, PokeEventDto.from(poke));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
    }
}
