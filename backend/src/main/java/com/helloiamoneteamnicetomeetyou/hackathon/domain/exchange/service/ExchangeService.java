package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.ZoneResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
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
    private final SseEventPublisher sseEventPublisher;

    /**
     * 교환을 만든다.
     *
     * <p>장소는 부스의 첫 구역으로 정해 둔다. 이번 행사는 교환 자리가 한 곳이라 고르는 화면이
     * 아니라 확인하는 화면이다. 자리가 여러 곳이 되면 여기에 고르는 규칙이 들어온다.
     *
     * <p>격자 시작점을 여기서 한 번 정하는 것이 이 메서드의 핵심이다. 참가자들이 각자 자기 시계로
     * 격자를 만들면 같은 칸 번호가 서로 다른 시각을 뜻하게 된다.
     */
    @Transactional
    public ExchangeResponseDto create(Long boothId, ExchangeType type, List<UUID> participantUserIds) {
        List<UUID> distinctIds = participantUserIds.stream().distinct().toList();

        if (distinctIds.size() < 2) {
            throw new ApplicationException(ErrorCode.INVALID_EXCHANGE_PARTICIPANTS);
        }

        Zone zone = zoneRepository.findByBoothIdOrderByIdAsc(boothId).stream()
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.ZONE_NOT_FOUND));

        Exchange exchange = exchangeRepository.save(
                Exchange.of(zone, type, TimeSlotGrid.baseTimeFrom(LocalDateTime.now())));
        assignFreeIdentity(exchange);

        for (UUID userId : distinctIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
            participantRepository.save(ExchangeParticipant.of(exchange, user));
        }

        notifyParticipants(exchange.getId(), distinctIds, SseEventType.EXCHANGE_CREATED);

        return toResponse(exchange);
    }

    public ExchangeResponseDto find(Long exchangeId) {
        return toResponse(getExchange(exchangeId));
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

        timeSlotRepository.deleteAllByExchangeIdAndUserId(exchangeId, userId);
        timeSlotRepository.saveAll(
                distinctSlots.stream().map(slot -> ExchangeTimeSlot.of(exchange, user, slot)).toList());

        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_TIME_UPDATED);

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

        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_TIME_UPDATED);

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

        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_TIME_UPDATED);

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
     * <p>카드 주인은 아직 바꾸지 않는다. 무엇을 주고받는지는 매칭이 정하는 값이라 서버가 모른다.
     */
    @Transactional
    public ExchangeResponseDto complete(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        getParticipant(exchangeId, userId);

        exchange.complete();

        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_COMPLETED);

        return toResponse(exchange);
    }

    /**
     * 약속을 취소한다. 참가자 누구든 취소할 수 있다.
     *
     * <p><b>상대에게 알리는 것이 이 API 의 존재 이유다.</b> 취소한 사람 화면에서만 사라지면 상대는
     * 오지 않을 사람을 계속 기다리게 된다.
     */
    @Transactional
    public ExchangeResponseDto cancel(Long exchangeId, UUID userId) {
        Exchange exchange = getExchange(exchangeId);
        getParticipant(exchangeId, userId);

        exchange.cancel();
        timeSlotRepository.deleteAllByExchangeId(exchangeId);

        notifyParticipants(exchangeId, participantIds(exchangeId), SseEventType.EXCHANGE_CANCELLED);

        return toResponse(exchange);
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
    private void assignFreeIdentity(Exchange exchange) {
        Set<Integer> used = new HashSet<>(exchangeRepository.findIdentityCodesByStatuses(ACTIVE_STATUSES));
        int start = Math.floorMod(exchange.getId() * 37, IDENTITY_CAPACITY);

        for (int step = 0; step < IDENTITY_CAPACITY; step++) {
            int index = (start + step) % IDENTITY_CAPACITY;
            int mark = index / IDENTITY_NUMBER_COUNT;
            int number = IDENTITY_NUMBER_MIN + index % IDENTITY_NUMBER_COUNT;

            if (!used.contains(mark * 100 + number)) {
                exchange.assignIdentity(mark, number);
                return;
            }
        }

        // 720개를 동시에 쓰고 있다는 뜻이라 이 행사 규모에서는 오지 않는 자리다. 그래도 교환을
        // 못 만들게 막지는 않는다. 식별 화면이 헷갈리는 것보다 교환이 안 되는 쪽이 더 나쁘다.
        log.warn("식별자를 모두 쓰고 있어 겹치는 값을 준다: exchangeId={}", exchange.getId());
        exchange.assignIdentity(start / IDENTITY_NUMBER_COUNT, IDENTITY_NUMBER_MIN + start % IDENTITY_NUMBER_COUNT);
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
     * <p>누른 사람은 응답으로 최신 상태를 이미 받았으니 한 번 더 읽는 셈이지만, 같은 사람이 탭을
     * 여러 개 열어 뒀을 때 나머지 탭이 갱신되려면 이 편이 맞다.
     */
    private void notifyParticipants(Long exchangeId, List<UUID> userIds, SseEventType type) {
        Map<String, Object> data = Map.of("exchangeId", exchangeId);

        userIds.forEach(userId -> sseEventPublisher.toUser(userId, type, data));
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
