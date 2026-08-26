package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.ZoneResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 교환 약속의 장소와 시간을 다룬다.
 *
 * <p><b>{@link #create} 는 매칭 알고리즘이 붙을 자리다.</b> 지금은 화면이 매칭을 목업으로 돌린
 * 뒤 임시 엔드포인트로 이걸 부르지만, 서버가 상대를 찾게 되면 그쪽에서 같은 메서드를 부르면 된다.
 * 이 클래스는 "누구와 교환하는지" 를 정하지 않고 "정해진 사람들의 약속" 만 다룬다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeService {

    /**
     * 식별 화면에서 쓸 표시의 가짓수다. 시안이 정한 과일 5종(레몬, 사과, 체리, 수박, 복숭아)에
     * 맞춘다. 화면이 이 범위의 번호마다 그림과 색을 정해 두고 있어서, 바꾸려면 프론트의
     * {@code IDENTITY_MARKS} 표도 같이 바꿔야 한다.
     */
    private static final int IDENTITY_MARK_COUNT = 5;

    /** 식별 번호는 두 자리로 둔다. 시안의 "레몬 28" 에서 28 자리다. */
    private static final int IDENTITY_NUMBER_MIN = 10;
    private static final int IDENTITY_NUMBER_COUNT = 90;

    /** 표시 5가지 × 번호 90가지. 이만큼의 교환이 동시에 진행될 때까지는 겹치지 않는다. */
    private static final int IDENTITY_CAPACITY = IDENTITY_MARK_COUNT * IDENTITY_NUMBER_COUNT;

    private static final List<ExchangeStatus> ACTIVE_STATUSES =
            List.of(ExchangeStatus.PENDING, ExchangeStatus.IN_PROGRESS);

    private final ExchangeRepository exchangeRepository;
    private final ExchangeParticipantRepository participantRepository;
    private final ExchangeTimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final SseEventPublisher sseEventPublisher;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 교환을 만든다.
     *
     * <p>장소는 부스의 첫 구역으로 정해 둔다. 이번 행사는 교환 자리가 한 곳이라 고르는 화면이
     * 아니라 확인하는 화면이다. 자리가 여러 곳이 되면 여기에 고르는 규칙이 들어온다.
     *
     * <p>격자 시작점을 여기서 한 번 정하는 것이 이 메서드의 핵심이다. 참가자들이 각자 자기 시계로
     * 격자를 만들면 같은 칸 번호가 서로 다른 시각을 뜻하게 된다.
     */
    /**
     * 교환을 만들고 엔티티를 돌려준다.
     *
     * <p>만든 교환에 곧바로 다른 것을 붙여야 하는 쪽이 쓴다. 찔러보기가 성사되면 교환을 만든 뒤
     * 주고받을 카드를 이어 붙이는데, 그때 응답 DTO 가 아니라 엔티티가 필요하다.
     *
     * <p><b>교환을 만드는 길은 여기 하나다.</b> 장소와 격자 시작점, 식별자가 전부 여기서 붙는다.
     * 다른 데서 {@code Exchange} 를 직접 만들면 그것들이 비어서 약속 화면이 깨진다.
     */
    @Transactional
    public Exchange createExchange(Long boothId, ExchangeType type, List<UUID> participantUserIds) {
        List<UUID> distinctIds = participantUserIds.stream().distinct().toList();

        if (distinctIds.size() < 2) {
            throw new ApplicationException(ErrorCode.INVALID_EXCHANGE_PARTICIPANTS);
        }

        Zone zone = zoneRepository.findByBoothIdOrderByIdAsc(boothId).stream()
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.ZONE_NOT_FOUND));

        Exchange exchange = exchangeRepository.save(Exchange.create(type));
        prepareAppointment(exchange, boothId);

        for (UUID userId : distinctIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
            participantRepository.save(ExchangeParticipant.accepted(exchange, user));
        }

        notifyParticipants(exchange.getId(), distinctIds, SseEventType.EXCHANGE_CREATED);

        return exchange;
    }

    /**
     * 매칭 결과를 보고 장소를 잡으러 들어간다.
     *
     * <p>상대가 아직 수락 전이어도 바로 IN_PROGRESS 로 옮긴다. 지금 화면 흐름은 양쪽이 서로의
     * 수락을 기다리지 않고 각자 장소·시간 화면으로 들어가 맞춰 보는 방식이라, "둘 다 눌러야
     * 진행 중" 같은 조건을 걸 이유가 없다.
     */
    @Transactional
    public void accept(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        getParticipant(exchangeId, userId).accept();

        if (exchange.getStatus() == ExchangeStatus.PENDING) {
            exchange.startProgress();
        }

        // 매칭은 만날 자리도 시간도 모른 채 교환을 만든다. 약속 화면이 필요한 것은 여기서 붙는다.
        // 먼저 수락한 사람이 정하고, 늦게 수락한 사람은 같은 값을 그대로 본다.
        prepareAppointment(exchange, boothIdOf(exchange));
    }

    /**
     * 매칭 결과를 거절한다. 이 교환은 참가자 전원에게 끝난 거래가 되고, 거절한 사람을 뺀
     * 나머지는 다시 상대를 찾아야 한다.
     *
     * <p>재매칭은 {@link MatchTriggerEvent} 로 미룬다. 카드 등록 때와 같은 이유다 —
     * {@code runMatching} 이 비동기라 이 트랜잭션의 커밋보다 먼저 돌면 방금 풀어 준 카드가
     * 아직 예약 중인 채로 재매칭에 잡혀 후보에서 빠진다.
     */
    @Transactional
    public void reject(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        if (exchange.getStatus() != ExchangeStatus.PENDING && exchange.getStatus() != ExchangeStatus.IN_PROGRESS) {
            return;
        }

        List<ExchangeParticipant> participants = participantRepository.findAllByExchangeId(exchangeId);
        participants.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_EXCHANGE_PARTICIPANT))
                .reject();
        exchange.cancel();

        releaseReservations(exchangeId);

        for (ExchangeParticipant participant : participants) {
            UUID participantId = participant.getUser().getId();
            if (!participantId.equals(userId)) {
                sseEventPublisher.toUser(participantId, SseEventType.MATCH_REJECTED, Map.of("exchangeId", exchangeId));
            }
            eventPublisher.publishEvent(new MatchTriggerEvent(participantId));
        }
    }

    /**
     * 이 교환이 잡아 둔 카드를 전부 풀어 다시 매칭 후보로 돌려놓는다.
     *
     * <p>거절과 취소가 함께 쓴다. 풀지 않으면 카드가 {@code RESERVED} 에 갇혀 그 사람은 다시는
     * 매칭되지 않는다 — 매칭 쿼리가 {@code status = 'LEFT'} 인 카드만 본다.
     */
    private void releaseReservations(Long exchangeId) {
        for (ExchangeItem item : exchangeItemRepository.findByExchangeId(exchangeId)) {
            userHaveItemRepository.findByUserIdAndItemId(item.getFromUser().getId(), item.getItem().getId())
                    .ifPresent(UserHaveItem::cancelReservation);
        }
    }

    /**
     * 성사된 교환의 카드를 양쪽에서 덜어낸다.
     *
     * <p>주는 쪽은 {@code quantityLeft} 가 줄고 다 나가면 {@code OUT} 이 된다. 받는 쪽은 찾는
     * 개수가 줄고 0 이 되면 희망 목록에서 아예 빠진다.
     */
    private void consumeItems(Long exchangeId) {
        for (ExchangeItem item : exchangeItemRepository.findByExchangeId(exchangeId)) {
            Long itemId = item.getItem().getId();
            int quantity = item.getQuantity();

            userHaveItemRepository.findByUserIdAndItemId(item.getFromUser().getId(), itemId)
                    .ifPresent(have -> have.completeExchange(quantity));

            userWantItemRepository.findByUserIdAndItemId(item.getToUser().getId(), itemId)
                    .ifPresent(want -> {
                        if (want.decrease(quantity)) {
                            userWantItemRepository.delete(want);
                        }
                    });
        }
    }

    /**
     * 약속 화면이 보는 교환 하나.
     *
     * <p>아직 아무도 수락하지 않은 교환은 만날 자리가 없어서 약속으로 읽을 수 없다. 그때
     * {@code null} 필드로 내려보내면 화면이 뒤늦게 깨지므로, 여기서 끊고 알려준다.
     */
    public ExchangeResponseDto find(Long exchangeId) {
        Exchange exchange = getExchange(exchangeId);
        if (!exchange.hasAppointment()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_NOT_ACCEPTED);
        }
        return toResponse(exchange);
    }

    /**
     * 이 사람이 지금 잡고 있는 약속. 없으면 {@code null} 이다.
     *
     * <p>앱을 다시 열거나 새로고침하면 화면이 들고 있던 것이 전부 사라진다. 그때 이걸 불러서
     * 진행 중인 약속으로 돌아온다. 실시간 알림만으로는 부족한데, 끊겨 있던 동안 온 알림은 다시
     * 오지 않기 때문이다.
     */
    public ExchangeResponseDto findActiveOf(UUID userId) {
        return exchangeRepository.findActiveByUserId(userId, ACTIVE_STATUSES).stream()
                // 매칭이 방금 제안한 교환은 아직 자리도 시간도 없다. 그건 매칭 화면이 다룰 몫이라
                // 여기서는 건너뛴다. 수락해서 약속이 잡힌 것만 돌려준다.
                .filter(Exchange::hasAppointment)
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * 내가 고른 칸을 통째로 덮어쓴다.
     *
     * <p>시간이 이미 확정된 뒤에는 받지 않는다. 확정된 약속의 칸을 누가 뒤늦게 바꾸면 화면마다
     * 다른 시각이 보이게 된다.
     */
    @Transactional
    public ExchangeResponseDto updateTimeSlots(Long exchangeId, UUID userId, List<Integer> slots) {
        Exchange exchange = getExchange(exchangeId);

        // 참가자인지를 먼저 본다. 상태를 먼저 보면 남의 약속을 건드린 사람이 "이미 정해졌다" 는
        // 답을 받게 되는데, 그건 자기와 상관없는 약속의 상태를 알려 주는 것이라 답으로도 틀렸다.
        User user = getParticipant(exchangeId, userId).getUser();

        if (exchange.isTimeConfirmed()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_TIME_ALREADY_CONFIRMED);
        }

        List<Integer> distinctSlots = slots.stream().distinct().sorted().toList();
        TimeSlotGrid.validateAll(distinctSlots);

        boolean matchedBefore = earliestOverlap(exchangeId) != null;

        timeSlotRepository.deleteAllByExchangeIdAndUserId(exchangeId, userId);
        timeSlotRepository.saveAll(
                distinctSlots.stream().map(slot -> ExchangeTimeSlot.of(exchange, user, slot)).toList());

        // 화면을 맞추는 것은 저장될 때마다 해야 한다. 알림이 아니라서 전원에게 보낸다. 누른
        // 사람도 받아야 같은 사람이 탭을 여러 개 열어 뒀을 때 나머지 탭이 따라온다.
        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_SLOTS_UPDATED);

        boolean matchedAfter = earliestOverlap(exchangeId) != null;

        /*
          시안(204:5230)의 조건이 "상대 사용자가 시간을 입력 완료 & 일치하는 시간이 존재하는
          경우" 다. 상대가 볼 알림은 내가 무엇을 골랐는지가 아니라 시간이 맞았는지 안 맞았는지다.

          겹치는 칸의 유무가 실제로 바뀐 경우에만 보낸다. 저장될 때마다 보내면 시간표는 칸을
          누를 때마다 저장되기 때문에, 상대가 다섯 칸을 고르는 동안 "시간 매칭에 실패했어요" 가
          다섯 건 쌓인다. 상대가 한 칸도 고르지 않았으면 겹칠 일 자체가 없어 유무가 바뀌지
          않으므로, 시안이 말하는 "상대가 입력 완료" 조건과 결과가 같아진다.
        */
        if (matchedBefore != matchedAfter) {
            notifyOthers(
                    exchangeId,
                    userId,
                    matchedAfter
                            ? SseEventType.EXCHANGE_TIME_MATCHED
                            : SseEventType.EXCHANGE_TIME_MISMATCHED);
        }

        return toResponse(exchange);
    }

    /**
     * 만날 자리를 바꾼다.
     *
     * <p>구역은 어드민이 만들고 고치고 지운다. 그래서 화면이 보낸 이름이나 좌표를 믿지 않고
     * {@code zoneId} 로 다시 읽는다. 화면이 목록을 받아 둔 사이에 어드민이 이름을 바꿨을 수
     * 있는데, 그때 화면이 들고 있던 옛 값을 저장하면 어드민이 고친 것이 되돌려진다.
     *
     * <p><b>같은 부스의 구역만 고를 수 있다.</b> 약도는 부스마다 다른 그림이고 좌표도 그 그림
     * 안에서의 비율이라, 다른 부스의 구역을 넣으면 핀이 엉뚱한 자리를 가리킨다.
     *
     * <p>바꾼 사람만 알면 소용없어서 나머지 참가자에게 알린다. 상대가 옛 자리에서 기다리는 것이
     * 이 기능에서 제일 나쁜 결과다. 바꾼 본인은 응답으로 최신 자리를 이미 받았고, 알림까지 가면
     * 자기가 방금 한 행동이 자기 알림함에 쌓인다.
     */
    @Transactional
    public ExchangeResponseDto updateZone(Long exchangeId, UUID userId, Long zoneId) {
        Exchange exchange = getExchange(exchangeId);

        // 참가자인지를 먼저 본다. 남의 약속을 건드린 사람에게 그 약속의 상태를 알려 주면 안 된다.
        getParticipant(exchangeId, userId);

        // 아직 아무도 수락하지 않은 교환은 자리가 안 붙어 있다. 그대로 두면 어느 부스인지 알 길이
        // 없어서 아래 부스 대조에서 터진다. 수락 전에는 옮길 자리 자체가 없다고 답하는 것이 맞다.
        if (!exchange.hasAppointment()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_NOT_ACCEPTED);
        }

        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ZONE_NOT_FOUND));

        Long boothId = exchange.getZone().getBooth().getId();
        if (!boothId.equals(zone.getBooth().getId())) {
            throw new ApplicationException(ErrorCode.ZONE_NOT_FOUND);
        }

        exchange.changeZone(zone);

        notifyOthers(exchangeId, userId, SseEventType.EXCHANGE_PLACE_UPDATED);

        return toResponse(exchange);
    }

    /**
     * 시간을 처음부터 다시 고른다. 화면의 "시간 조율 요청하기" 다.
     *
     * <p>내 것만 지우면 안 된다. 겹치는 칸이 없다는 것은 상대의 선택도 함께 봐야 알 수 있는
     * 결론이라, 한 명만 다시 고르면 여전히 안 맞을 가능성이 크다.
     */
    @Transactional
    public ExchangeResponseDto resetTimeSlots(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        getParticipant(exchangeId, userId);

        timeSlotRepository.deleteAllByExchangeId(exchangeId);
        exchange.resetTime();

        notifyOthers(exchangeId, userId, SseEventType.EXCHANGE_TIME_REQUESTED);

        return toResponse(exchange);
    }

    /**
     * 겹치는 가장 빠른 칸으로 약속을 확정한다.
     *
     * <p>참가자 중 아무나 한 명이 누르면 확정된다. 전원이 눌러야 한다고 두면 마지막 사람이 화면을
     * 닫고 있을 때 약속이 영영 안 잡힌다. 나머지 참가자는 실시간 알림으로 확정된 시각을 받는다.
     */
    @Transactional
    public ExchangeResponseDto confirmTime(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        getParticipant(exchangeId, userId);

        Integer overlap = earliestOverlap(exchangeId);
        if (overlap == null) {
            throw new ApplicationException(ErrorCode.NO_OVERLAPPING_TIME);
        }

        exchange.confirmTime(overlap);

        notifyOthers(exchangeId, userId, SseEventType.EXCHANGE_TIME_UPDATED);

        return toResponse(exchange);
    }

    /**
     * 약속 장소에 도착했다고 알린다.
     *
     * <p>상대 화면의 "이동중 / 도착" 배지가 이걸 봅니다. 도착은 시간이 바뀐 것이 아니라서
     * {@code EXCHANGE_ARRIVED} 로 따로 보낸다.
     *
     * <p>시간이 확정되기 전에는 받지 않는다. 만날 시각이 안 정해졌는데 도착할 자리가 없다.
     */
    @Transactional
    public ExchangeResponseDto arrive(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        ExchangeParticipant participant = getParticipant(exchangeId, userId);

        if (!exchange.isTimeConfirmed()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_TIME_NOT_CONFIRMED);
        }

        participant.arrive();

        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_ARRIVED);

        return toResponse(exchange);
    }

    /**
     * 만나서 교환을 끝냈다. 참가자 누구든 누를 수 있고, 먼저 누른 한 번만 반영된다.
     *
     * <p>여기서 카드가 실제로 오간 것으로 친다. 무엇을 주고받는지는 매칭이 정한
     * {@link ExchangeItem} 을 그대로 따른다. 주는 쪽은 보유 수량이 그만큼 줄고, 받는 쪽은 찾는
     * 수량이 그만큼 준다. 둘 다 하지 않으면 교환이 끝나자마자 같은 카드로 다시 매칭된다.
     */
    @Transactional
    public ExchangeResponseDto complete(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        getParticipant(exchangeId, userId);

        exchange.complete();
        consumeItems(exchangeId);

        for (ExchangeItem item : exchangeItemRepository.findByExchangeId(exchangeId)) {
            receive(item);
        }

        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_COMPLETED);

        return toResponse(exchange);
    }

    /**
     * 받은 사람의 보유 카드를 늘린다.
     *
     * <p><b>덜어내는 쪽은 {@link #consumeItems} 가 맡는다.</b> 주는 사람의 보유 수량과 받는
     * 사람의 찾는 수량은 거기서 한 번만 줄인다. 예전에는 여기서도 같은 일을 해서 완료 한 번에
     * 수량이 두 번 깎였다.
     *
     * <p>보유 카드에 더하는 것과 재교환 가능하게 만드는 것은 다른 문제다. {@link UserHaveItem#acquired}
     * 와 {@link UserHaveItem#receiveMore} 둘 다 받은 몫을 곧바로 매칭 후보로 올리지 않는다.
     */
    private void receive(ExchangeItem item) {
        User toUser = item.getToUser();
        Item receivedItem = item.getItem();
        int quantity = item.getQuantity();

        userHaveItemRepository.findByUserIdAndItemId(toUser.getId(), receivedItem.getId())
                .ifPresentOrElse(
                        existing -> existing.receiveMore(quantity),
                        () -> userHaveItemRepository.save(UserHaveItem.acquired(toUser, receivedItem, quantity)));
    }

    /**
     * 약속을 취소한다. 참가자 누구든 취소할 수 있다.
     *
     * <p><b>상대에게 알리는 것이 이 API 의 존재 이유다.</b> 취소한 사람 화면에서만 사라지면 상대는
     * 오지 않을 사람을 계속 기다리게 된다.
     *
     * <p>거절과 똑같이 카드 예약을 풀고 전원에게 재매칭을 건다. 예전에는 이 둘을 하지 않아서,
     * 약속을 한 번 취소하면 카드가 {@code RESERVED} 에 갇혀 그 사람은 다시는 매칭되지 않았다.
     */
    @Transactional
    public ExchangeResponseDto cancel(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        getParticipant(exchangeId, userId);

        exchange.cancel();
        timeSlotRepository.deleteAllByExchangeId(exchangeId);
        releaseReservations(exchangeId);

        List<UUID> participantIds = participantIds(exchangeId);
        notifyOthers(exchangeId, userId, SseEventType.EXCHANGE_CANCELLED);
        for (UUID participantId : participantIds) {
            eventPublisher.publishEvent(new MatchTriggerEvent(participantId));
        }

        return toResponse(exchange);
    }

    /**
     * 만날 자리와 시간 격자, 약속 식별자를 붙인다.
     *
     * <p>교환을 만드는 길이 셋이다. 매칭, 찔러보기, 그리고 임시 엔드포인트. 어디로 들어오든
     * 약속을 잡으려면 이 셋이 있어야 해서 한곳에 모았다.
     */
    private void prepareAppointment(Exchange exchange, Long boothId) {
        Zone zone = zoneRepository.findByBoothIdOrderByIdAsc(boothId).stream()
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.ZONE_NOT_FOUND));

        int[] identity = freeIdentity(exchange.getId());
        exchange.prepareAppointment(
                zone, TimeSlotGrid.baseTimeFrom(LocalDateTime.now()), identity[0], identity[1]);
    }

    /**
     * 교환이 어느 부스의 것인지. 참가자가 주고받는 카드를 보고 찾는다.
     *
     * <p>교환 자체는 부스를 들고 있지 않다. 만날 자리가 정해지면 그 구역을 통해 알 수 있지만,
     * 자리를 정하기 전에는 카드가 유일한 단서다.
     */
    private Long boothIdOf(Exchange exchange) {
        return exchangeItemRepository.findByExchangeId(exchange.getId()).stream()
                .findFirst()
                .map(item -> item.getItem().getBooth().getId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    /**
     * 진행 중인 어느 교환과도 겹치지 않는 식별자를 골라 넣는다.
     *
     * <p><b>겹치면 안 되는 이유가 화면에 있다.</b> 같은 화면을 든 사람이 내 상대라고 안내하기
     * 때문에, 두 교환이 같은 값을 들면 행사장에서 엉뚱한 사람과 서로를 상대로 착각한다.
     *
     * <p>끝나거나 취소된 교환은 후보에서 빠지므로 값이 저절로 다시 쓸 수 있게 된다. 시작 자리를
     * 교환 id 로 흩어 두는 것은, 늘 앞에서부터 고르면 먼저 만들어진 교환이 끝났을 때 방금 끝난
     * 값을 바로 다음 사람이 받게 되기 때문이다.
     *
     * <p>고르는 것과 저장하는 것이 한 트랜잭션 안에 있지만 DB 제약으로 막지는 않는다. 두 교환이
     * 같은 순간에 만들어지면 이론상 같은 값을 고를 수 있는데, 서버가 한 대이고 교환 생성이 초당
     * 수십 건씩 일어나는 흐름이 아니라 지금은 여기까지만 한다.
     */
    private int[] freeIdentity(Long exchangeId) {
        Set<Integer> used = new HashSet<>(exchangeRepository.findIdentityCodesByStatuses(ACTIVE_STATUSES));
        int start = Math.floorMod(exchangeId * 37, IDENTITY_CAPACITY);

        for (int step = 0; step < IDENTITY_CAPACITY; step++) {
            int index = (start + step) % IDENTITY_CAPACITY;
            int mark = index / IDENTITY_NUMBER_COUNT;
            int number = IDENTITY_NUMBER_MIN + index % IDENTITY_NUMBER_COUNT;

            if (!used.contains(mark * 100 + number)) {
                return new int[]{mark, number};
            }
        }

        // 720개를 동시에 쓰고 있다는 뜻이라 이 행사 규모에서는 오지 않는 자리다. 그래도 교환을
        // 못 만들게 막지는 않는다. 식별 화면이 헷갈리는 것보다 교환이 안 되는 쪽이 더 나쁘다.
        log.warn("식별자를 모두 쓰고 있어 겹치는 값을 준다: exchangeId={}", exchangeId);
        return new int[]{start / IDENTITY_NUMBER_COUNT, IDENTITY_NUMBER_MIN + start % IDENTITY_NUMBER_COUNT};
    }

    private Exchange getExchange(Long exchangeId) {
        return exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    /** 참가자가 아니면 막는다. 남의 약속 시간을 바꾸지 못하게 하는 유일한 문이다. */
    private ExchangeParticipant getParticipant(Long exchangeId, UUID userId) {
        return participantRepository.findAllByExchangeId(exchangeId).stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_EXCHANGE_PARTICIPANT));
    }

    private List<UUID> participantIds(Long exchangeId) {
        return participantRepository.findAllByExchangeId(exchangeId).stream()
                .map(p -> p.getUser().getId())
                .toList();
    }

    /**
     * 참가자별 선택. 아직 안 고른 사람도 빈 목록으로 들어간다.
     *
     * <p>참가자 목록을 인자로 받는 것은, 부르는 쪽이 이미 읽어 둔 것을 다시 읽지 않게 하기
     * 위해서다. 응답 하나를 만들면서 같은 조회가 두 번 나가던 것을 이걸로 막는다.
     */
    private Map<UUID, List<Integer>> slotsByUser(List<ExchangeParticipant> participants) {
        Map<UUID, List<Integer>> slots = new LinkedHashMap<>();

        for (ExchangeParticipant participant : participants) {
            slots.put(participant.getUser().getId(), new ArrayList<>());
        }

        if (participants.isEmpty()) {
            return slots;
        }

        Long exchangeId = participants.getFirst().getExchange().getId();
        for (ExchangeTimeSlot slot : timeSlotRepository.findAllByExchangeId(exchangeId)) {
            slots.computeIfAbsent(slot.getUser().getId(), id -> new ArrayList<>()).add(slot.getSlotIndex());
        }

        slots.values().forEach(Collections::sort);

        return slots;
    }

    private Integer earliestOverlap(Long exchangeId) {
        return TimeSlotGrid.earliestOverlap(
                slotsByUser(participantRepository.findAllByExchangeId(exchangeId)).values());
    }

    /**
     * 참가자 전원에게 알린다. 누른 사람도 포함한다.
     *
     * <p><b>{@code PushMessage} 에 문구가 없는, 화면 갱신용 이벤트에만 쓴다.</b> 문구가 있는
     * 이벤트를 이걸로 보내면 누른 사람 알림함에도 자기가 방금 한 행동이 쌓이고, 앱이 닫혀
     * 있으면 잠금화면 푸시까지 간다. 그런 이벤트는 {@link #notifyOthers} 를 쓴다.
     */
    private void notifyParticipants(Long exchangeId, List<UUID> userIds, SseEventType type) {
        Map<String, Object> data = Map.of("exchangeId", exchangeId);

        userIds.forEach(userId -> sseEventPublisher.toUser(userId, type, data));
    }

    /**
     * 누른 사람을 뺀 나머지 참가자에게만 알린다.
     *
     * <p>시안(204:5026)의 노출 조건이 전부 "상대 사용자가 ...한 경우" 다. 누른 사람은 응답으로
     * 최신 상태를 이미 받으니 알릴 것이 없다. {@code reject()} 도 같은 방식으로 본인을 뺀다.
     */
    private void notifyOthers(Long exchangeId, UUID actorId, SseEventType type) {
        Map<String, Object> data = Map.of("exchangeId", exchangeId);

        participantIds(exchangeId).stream()
                .filter(userId -> !userId.equals(actorId))
                .forEach(userId -> sseEventPublisher.toUser(userId, type, data));
    }

    private ExchangeResponseDto toResponse(Exchange exchange) {
        Long exchangeId = exchange.getId();
        List<ExchangeParticipant> found = participantRepository.findAllByExchangeId(exchangeId);
        Map<UUID, List<Integer>> slotsByUser = slotsByUser(found);

        List<ExchangeParticipantResponseDto> participants =
                found.stream()
                        .map(participant -> {
                            User user = participant.getUser();
                            List<Integer> slots = slotsByUser.getOrDefault(user.getId(), List.of());
                            return new ExchangeParticipantResponseDto(
                                    user.getId(),
                                    user.getUsername(),
                                    slots,
                                    !slots.isEmpty(),
                                    participant.hasArrived());
                        })
                        .toList();

        Zone zone = exchange.getZone();

        return new ExchangeResponseDto(
                exchangeId,
                zone.getBooth().getId(),
                exchange.getType(),
                exchange.getStatus(),
                ZoneResponseDto.from(zone),
                exchange.getSlotBaseTime(),
                TimeSlotGrid.SLOT_COUNT,
                TimeSlotGrid.SLOT_MINUTES,
                exchange.getIdentityMark(),
                exchange.getIdentityNumber(),
                participants,
                TimeSlotGrid.earliestOverlap(slotsByUser.values()),
                participants.stream().allMatch(ExchangeParticipantResponseDto::answered),
                exchange.getExchangeTime());
    }
}
