package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ExchangeView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ParticipantView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
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
                slotsByUser.getOrDefault(user.getId(), List.of()));
    }

    /** 더미 대신 수락한다. 참가자 전원의 화면이 바뀌어야 해서 SSE 를 같이 내보낸다. */
    @Transactional
    public void acceptAsDummy(Long participantId) {
        ExchangeParticipant participant = findDummyParticipant(participantId);
        participant.accept();
        publishToBooth(participant.getExchange(), SseEventType.MATCH_ACCEPTED);
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

    @Transactional
    public void rejectAsDummy(Long participantId) {
        ExchangeParticipant participant = findDummyParticipant(participantId);
        participant.reject();
        publishToBooth(participant.getExchange(), SseEventType.MATCH_REJECTED);
    }

    @Transactional
    public void cancel(Long exchangeId) {
        Exchange exchange = findExchange(exchangeId);
        exchange.cancelByAdmin();
        publishToBooth(exchange, SseEventType.EXCHANGE_CANCELLED);
    }

    @Transactional
    public void complete(Long exchangeId) {
        Exchange exchange = findExchange(exchangeId);
        exchange.completeByAdmin();
        publishToBooth(exchange, SseEventType.EXCHANGE_COMPLETED);
    }

    private ExchangeParticipant findDummyParticipant(Long participantId) {
        ExchangeParticipant participant = exchangeParticipantRepository.findById(participantId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND));

        if (!participant.getUser().isAdminManaged()) {
            throw new ApplicationException(ErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        return participant;
    }

    private Exchange findExchange(Long exchangeId) {
        return exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    /**
     * 구역이 정해지지 않은 교환은 어느 부스로 보낼지 알 수 없어서 아무것도 보내지 않는다.
     *
     * <p>화면은 이벤트 내용을 믿지 않고 조회 API 를 다시 부르는 방식이라, 못 받은 쪽은 다음에
     * 화면을 열 때 맞는 상태를 읽게 된다.
     */
    private void publishToBooth(Exchange exchange, SseEventType type) {
        if (exchange.getZone() == null) {
            return;
        }
        sseEventPublisher.toBooth(exchange.getZone().getBooth().getId(), type, Map.of("exchangeId", exchange.getId()));
    }
}
