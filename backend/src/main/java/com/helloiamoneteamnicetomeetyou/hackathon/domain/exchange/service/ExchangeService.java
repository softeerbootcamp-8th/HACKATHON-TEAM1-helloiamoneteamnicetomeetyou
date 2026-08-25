package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.ZoneResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeParticipantResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 교환 약속의 장소와 시간을 다룬다.
 *
 * <p><b>{@link #create} 는 매칭 알고리즘이 붙을 자리다.</b> 지금은 화면이 매칭을 목업으로 돌린
 * 뒤 임시 엔드포인트로 이걸 부르지만, 서버가 상대를 찾게 되면 그쪽에서 같은 메서드를 부르면 된다.
 * 이 클래스는 "누구와 교환하는지" 를 정하지 않고 "정해진 사람들의 약속" 만 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeService {

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
                                    user.getId(), user.getUsername(), slots, !slots.isEmpty());
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
                participants,
                TimeSlotGrid.earliestOverlap(slotsByUser.values()),
                participants.stream().allMatch(ExchangeParticipantResponseDto::answered),
                exchange.getExchangeTime());
    }
}
