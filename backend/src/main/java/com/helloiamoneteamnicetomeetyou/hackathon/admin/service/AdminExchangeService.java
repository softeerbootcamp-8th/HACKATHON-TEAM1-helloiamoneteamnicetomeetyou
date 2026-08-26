package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ExchangeView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ParticipantView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto.ExchangeEventDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event.MatchTriggerEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 교환 현황을 보고, 막힌 건에 손을 댄다.
 *
 * <p><b>대리 조작은 더미 사용자에게만 연다.</b> 실제 참가자의 수락을 운영자가 대신 눌러 버리면
 * 그 사람이 하지 않은 일이 그 사람 이름으로 남는다. 부스에서 흐름을 이어 주려고 만든 기능이
 * 참가자 기록을 덮어쓰는 도구가 되면 안 된다.
 *
 * <p><b>시간 칸을 넣는 것은 {@link ExchangeService#updateTimeSlots} 를 그대로 부른다.</b>
 * 사용자가 자기 화면에서 고르는 것과 완전히 같은 경로라, 검증도 저장도 SSE 도 따라온다.
 * 어드민에서 격자 계산을 다시 만들면 어드민으로 넣은 칸만 다르게 저장되는 길이 생긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;
    private final ExchangeTimeSlotRepository exchangeTimeSlotRepository;
    private final ExchangeService exchangeService;
    private final ApplicationEventPublisher eventPublisher;
    private final SseEventPublisher sseEventPublisher;

    public List<ExchangeView> findExchanges() {
        List<Exchange> exchanges = exchangeRepository.findAllForAdmin();
        if (exchanges.isEmpty()) {
            return List.of();
        }

        List<Long> exchangeIds = exchanges.stream().map(Exchange::getId).toList();

        Map<Long, List<ExchangeParticipant>> participantsByExchange =
                exchangeParticipantRepository.findAllByExchangeIdIn(exchangeIds).stream()
                        .collect(Collectors.groupingBy(participant -> participant.getExchange().getId()));

        Map<Long, Map<UUID, List<Integer>>> slotsByExchange = slotsByExchange(exchangeIds);

        return exchanges.stream()
                .map(exchange -> toView(
                        exchange,
                        participantsByExchange.getOrDefault(exchange.getId(), List.of()),
                        slotsByExchange.getOrDefault(exchange.getId(), Map.of())))
                .toList();
    }

    /**
     * 교환별, 사람별 고른 칸. 한 번에 읽어서 나눈다.
     *
     * <p>교환마다 따로 물으면 화면에 뜬 카드 수만큼 쿼리가 늘어난다.
     */
    private Map<Long, Map<UUID, List<Integer>>> slotsByExchange(List<Long> exchangeIds) {
        Map<Long, Map<UUID, List<Integer>>> slots = new LinkedHashMap<>();

        for (ExchangeTimeSlot slot : exchangeTimeSlotRepository.findAllByExchangeIdIn(exchangeIds)) {
            slots.computeIfAbsent(slot.getExchange().getId(), id -> new LinkedHashMap<>())
                    .computeIfAbsent(slot.getUser().getId(), id -> new ArrayList<>())
                    .add(slot.getSlotIndex());
        }

        slots.values().forEach(byUser -> byUser.values().forEach(Collections::sort));

        return slots;
    }

    /** 오른쪽 패널에서 "이 사람이 낀 약속" 을 보여 줄 때 쓴다. */
    public List<ExchangeView> findExchangesOf(java.util.UUID userId) {
        return findExchanges().stream()
                .filter(view -> view.participants().stream().anyMatch(p -> p.userId().equals(userId)))
                .toList();
    }

    private ExchangeView toView(
            Exchange exchange, List<ExchangeParticipant> participants, Map<UUID, List<Integer>> slotsByUser) {

        List<ParticipantView> views = participants.stream()
                .map(participant -> toParticipantView(participant, slotsByUser))
                .toList();

        return new ExchangeView(
                exchange.getId(),
                exchange.getStatus(),
                exchange.getType().name(),
                exchange.getZone() == null ? null : exchange.getZone().getName(),
                exchange.getZone() == null ? null : exchange.getZone().getBooth().getName(),
                exchange.getExchangeTime(),
                exchange.getCreatedAt(),
                exchange.getSlotBaseTime(),
                TimeSlotGrid.earliestOverlap(views.stream().map(ParticipantView::slots).toList()),
                views);
    }

    private ParticipantView toParticipantView(ExchangeParticipant participant, Map<UUID, List<Integer>> slotsByUser) {
        User user = participant.getUser();
        return new ParticipantView(
                participant.getId(),
                user.getId(),
                user.getId().toString().substring(0, 8),
                user.getUsername(),
                participant.getStatus(),
                user.isAdminManaged(),
                participant.hasArrived(),
                slotsByUser.getOrDefault(user.getId(), List.of()));
    }

    /**
     * 더미 대신 수락한다.
     *
     * <p><b>{@link ExchangeService#accept} 를 그대로 부른다.</b> 예전에는 여기서
     * {@code participant.accept()} 만 했는데, 그러면 {@code startProgress()} 와
     * {@code prepareAppointment()} 가 빠져서 만날 자리도 시간 격자도 식별자도 안 붙었다.
     * 교환이 "진행 중 · 장소 미정 · 시간 미정" 으로 남고 시간 격자도 뜨지 않는 원인이었다.
     */
    @Transactional
    public void acceptAsDummy(Long participantId) {
        ExchangeParticipant participant = findDummyParticipant(participantId);

        exchangeService.accept(participant.getExchange().getId(), participant.getUser().getId());
        notifyParticipants(participant.getExchange(), SseEventType.MATCH_ACCEPTED);
    }

    /**
     * 더미가 고른 시간 칸을 운영자가 대신 넣는다.
     *
     * <p><b>사용자 화면과 같은 길로 보낸다.</b> {@link ExchangeService#updateTimeSlots} 가 참가자
     * 확인, 확정 여부, 칸 번호 검증, 저장, 그리고 참가자 전원에게 나가는
     * {@code EXCHANGE_TIME_UPDATED} 까지 전부 한다. 여기서 다시 만들면 어드민으로 넣은 칸만
     * 다르게 저장되는 길이 생긴다.
     *
     * <p>체크를 하나도 안 하면 폼이 {@code slots} 를 아예 안 보낸다. 그것을 빈 목록으로 다루지
     * 않으면 "고른 것 전부 지우기" 를 할 방법이 없어진다.
     */
    @Transactional
    public void updateTimeSlotsAsDummy(Long participantId, List<Integer> slots) {
        ExchangeParticipant participant = findDummyParticipant(participantId);

        exchangeService.updateTimeSlots(
                participant.getExchange().getId(),
                participant.getUser().getId(),
                slots == null ? List.of() : slots);
    }

    /**
     * 더미 대신 거절한다.
     *
     * <p>{@link ExchangeService#reject} 를 부른다. 참가자 상태만 바꾸던 예전 방식은 교환을
     * 정리하지 않아서, 예약된 카드가 계속 묶여 있고 남은 사람이 재매칭도 못 받았다. 그쪽 경로는
     * 카드 예약을 풀고 남은 사람에게 재매칭을 걸어 준다.
     */
    @Transactional
    public void rejectAsDummy(Long participantId) {
        ExchangeParticipant participant = findDummyParticipant(participantId);

        exchangeService.reject(participant.getExchange().getId(), participant.getUser().getId());
    }

    /**
     * 더미 대신 도착했다고 표시한다.
     *
     * <p>상대 화면의 "이동중 / 도착" 배지가 이걸 본다. 실기기 한 대로 시연할 때 더미 쪽 도착을
     * 누를 사람이 없어서 배지가 영영 "이동중" 에 멈춰 있었다.
     */
    @Transactional
    public void arriveAsDummy(Long participantId) {
        ExchangeParticipant participant = findDummyParticipant(participantId);

        exchangeService.arrive(participant.getExchange().getId(), participant.getUser().getId());
    }

    /**
     * 겹치는 가장 빠른 칸으로 약속을 확정한다.
     *
     * <p><b>참가자 전원의 이름으로 부른다.</b> 사용자 화면의 확정은 전원이 눌러야 되는데, 더미는
     * 화면을 들고 있는 사람이 없어서 스스로 누를 수가 없다. 한 명 이름으로만 부르면 약속이
     * 확정되지 않고 "상대의 확정을 기다리는 중" 에서 멈춘다. 이미 누른 사람은 그쪽에서 걸러진다.
     *
     * <p>겹치는 칸이 없으면 {@code NO_OVERLAPPING_TIME} 으로 거절된다.
     */
    @Transactional
    public void confirmTime(Long exchangeId) {
        for (UUID userId : participantIds(exchangeId)) {
            exchangeService.confirmTime(exchangeId, userId);
        }
    }

    /**
     * 만날 자리를 옮긴다. 부스에서 원래 자리가 붐비거나 막혔을 때 운영자가 옮겨 준다.
     *
     * <p><b>자리를 옮길 수 있는 곳은 여기뿐이다.</b> 사용자 화면은 미리 정해 둔 자리를 확인만
     * 한다. {@link ExchangeService#updateZoneByAdmin} 을 그대로 부르고, 같은 부스의 구역인지
     * 확인하는 것과 참가자 전원에게 {@code EXCHANGE_PLACE_UPDATED} 를 보내는 것이 전부 그쪽에 있다.
     *
     * <p>아직 아무도 수락하지 않은 교환은 자리가 안 붙어 있어서 거절된다. 옮길 자리 자체가 없다.
     */
    @Transactional
    public void updatePlace(Long exchangeId, Long zoneId) {
        exchangeService.updateZoneByAdmin(exchangeId, zoneId);
    }

    /**
     * 이 사람의 매칭을 다시 돌린다.
     *
     * <p>카드는 맞는데 매칭이 안 붙어 있는 상태를 부스에서 풀어 주는 자리다. 카드를 뗐다
     * 붙이지 않고도 다시 시도할 수 있다.
     *
     * <p>이미 진행 중인 교환이 있으면 {@code runMatching} 이 그냥 지나간다. 한 사람은 동시에
     * 하나의 매칭만 갖는다는 규칙이라, 먼저 그 교환을 취소해야 새 상대를 찾는다.
     */
    @Transactional
    public void rematch(java.util.UUID userId) {
        eventPublisher.publishEvent(new MatchTriggerEvent(userId));
    }

    /**
     * 막힌 교환을 끊는다. 잡아 둔 카드도 같이 풀어 준다.
     *
     * <p>풀지 않으면 어드민이 정리해 준 그 카드가 예약된 개수만큼 영영 매칭에서 빠진다. 끊는
     * 목적이 그 사람을 다시 시연에 넣는 것이라 정반대의 결과가 된다.
     *
     * <p>이미 끝났거나 취소된 교환에는 풀 것이 없다. 완료된 교환에 예약을 돌려주면 이미 상대에게
     * 넘어간 카드가 되살아난다.
     */
    @Transactional
    public void cancel(Long exchangeId) {
        Exchange exchange = findExchange(exchangeId);
        boolean wasActive = exchange.isActive();

        exchange.cancelByAdmin();
        if (wasActive) {
            exchangeService.releaseReservations(exchangeId);
        }

        notifyParticipants(exchange, SseEventType.EXCHANGE_CANCELLED);
    }

    /**
     * 실물 교환은 끝났는데 화면에서 완료 처리가 안 된 건을 닫는다. 카드도 같이 넘긴다.
     *
     * <p>넘기지 않으면 잠가 둔 카드가 풀리지도 상대에게 가지도 않은 채 남아서, 어드민이 닫아 준
     * 사람이 오히려 다시 매칭되지 않는다.
     *
     * <p>이미 끝났거나 취소된 교환은 건드리지 않는다. 두 번 넘기면 개수가 두 번 깎인다.
     */
    @Transactional
    public void complete(Long exchangeId) {
        Exchange exchange = findExchange(exchangeId);
        boolean wasActive = exchange.isActive();

        exchange.completeByAdmin();
        if (wasActive) {
            exchangeService.settleItems(exchangeId);
        }

        notifyParticipants(exchange, SseEventType.EXCHANGE_COMPLETED);
    }

    private ExchangeParticipant findDummyParticipant(Long participantId) {
        ExchangeParticipant participant = exchangeParticipantRepository.findById(participantId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND));

        if (!participant.getUser().isAdminManaged()) {
            throw new ApplicationException(ErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        return participant;
    }

    /**
     * 참가자 한 명의 id. 확정처럼 "누구든 한 명이 누르면 되는" 동작에 쓴다.
     *
     * <p>서비스가 참가자인지 확인하는 문을 지나가야 하는데, 어드민은 참가자가 아니라서 대신
     * 세울 이름이 필요하다. 누구를 세우든 결과가 같은 동작에만 쓴다.
     */
    private List<UUID> participantIds(Long exchangeId) {
        List<UUID> userIds = exchangeParticipantRepository.findAllByExchangeId(exchangeId).stream()
                .map(participant -> participant.getUser().getId())
                .toList();

        if (userIds.isEmpty()) {
            throw new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND);
        }

        return userIds;
    }

    private Exchange findExchange(Long exchangeId) {
        return exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    /**
     * 참가자 전원에게 보낸다.
     *
     * <p><b>부스가 아니라 사람에게 보낸다.</b> 예전에는 구역에서 부스를 찾아 부스 전체에 뿌렸는데,
     * 매칭이 만든 교환은 아직 구역이 없어서({@code MatchingService} 가 자리를 잡지 않은 채로
     * 만든다) 조건에 걸려 <b>아무것도 안 나갔다.</b> 오류도 안 나서 부스에서는 "어드민에서 눌렀는데
     * 상대 화면이 그대로" 로만 보였다.
     *
     * <p>참가자에게 직접 보내면 구역이 있든 없든 닿는다. {@code ExchangeService} 가 쓰는 방식과도
     * 같아서, 어드민으로 한 것과 사용자가 한 것이 화면에서 구분되지 않는다.
     */
    private void notifyParticipants(Exchange exchange, SseEventType type) {
        ExchangeEventDto data = new ExchangeEventDto(exchange.getId());

        exchangeParticipantRepository.findAllByExchangeId(exchange.getId())
                .forEach(participant -> sseEventPublisher.toUser(participant.getUser().getId(), type, data));
    }
}
