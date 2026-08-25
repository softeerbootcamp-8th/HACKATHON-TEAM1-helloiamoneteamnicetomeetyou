package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.demo;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service.ExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository.ExchangeTimeSlotRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEvent;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 더미 상대가 시간을 고르는 것을 대신한다. 혼자 시연할 때 화면이 멈추지 않게 하는 용도다.
 *
 * <p><b>{@code demo.auto-respond: false} 로 끈다.</b> 실제 두 기기를 붙여 시연할 때는 꺼야 진짜
 * 사람의 선택만 보인다.
 *
 * <p>흉내내는 것은 <b>언제 응답하느냐</b>뿐이다. 고른 칸은 정식 API 를 통해 실제
 * {@code exchange_time_slots} 에 저장되고, 상대 화면은 그것을 DB 에서 읽는다.
 *
 * <p>정식 코드가 이 클래스를 부르지 않는다. {@link SseEvent} 를 엿듣는 방식이라 의존이 한쪽으로만
 * 흐르고, 이 패키지를 지우면 그대로 없어진다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "demo.auto-respond", havingValue = "true", matchIfMissing = true)
public class DemoParticipantResponder {

    /** 사람이 고른 뒤 상대가 답하기까지의 간격. 즉시 답하면 기다리는 화면을 볼 수 없다. */
    private static final long DELAY_MS = 1500;

    private final ExchangeService exchangeService;
    private final ExchangeParticipantRepository participantRepository;
    private final ExchangeTimeSlotRepository timeSlotRepository;
    private final long delayMs;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "demo-responder");
                thread.setDaemon(true);
                return thread;
            });

    public DemoParticipantResponder(
            ExchangeService exchangeService,
            ExchangeParticipantRepository participantRepository,
            ExchangeTimeSlotRepository timeSlotRepository,
            @Value("${demo.respond-delay-ms:" + DELAY_MS + "}") long delayMs) {
        this.exchangeService = exchangeService;
        this.participantRepository = participantRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.delayMs = delayMs;
    }

    /**
     * 누군가 시간을 고쳤다는 알림을 엿듣는다.
     *
     * <p>{@code AFTER_COMMIT} 이 아니라 그냥 {@code @EventListener} 인 것에 주의한다. 실제 전송을
     * 맡는 {@code SseEventDispatcher} 와 달리 여기서는 DB 를 바로 읽지 않고 예약만 걸기 때문에,
     * 커밋 전에 받아도 상관이 없다. 예약된 작업은 {@link #respond} 안에서 새 트랜잭션으로 읽는다.
     */
    @EventListener
    public void onSseEvent(SseEvent event) {
        if (event.type() != SseEventType.EXCHANGE_TIME_UPDATED) {
            return;
        }

        exchangeIdOf(event).ifPresent(exchangeId ->
                scheduler.schedule(() -> respondQuietly(exchangeId), delayMs, TimeUnit.MILLISECONDS));
    }

    private Optional<Long> exchangeIdOf(SseEvent event) {
        if (event.data() instanceof Map<?, ?> data && data.get("exchangeId") instanceof Long exchangeId) {
            return Optional.of(exchangeId);
        }

        return Optional.empty();
    }

    private void respondQuietly(Long exchangeId) {
        try {
            respond(exchangeId);
        } catch (Exception e) {
            // 데모용 코드가 죽어서 서버 로그를 어지럽히지 않게 여기서 끊는다.
            log.debug("더미 상대 응답 실패: exchangeId={}", exchangeId, e);
        }
    }

    /**
     * 아직 안 고른 더미 참가자들이 한꺼번에 답한다.
     *
     * <p><b>사람이 먼저 고른 뒤에만 답한다.</b> 그러지 않으면 사용자가 화면에 들어오기도 전에 상대
     * 줄이 채워져서, "아직 상대방을 기다려야 해요" 상태를 볼 수 없다.
     *
     * <p>한 번에 전부 답하는 것이 중요하다. 한 명씩 답하면 그 저장이 다시 알림을 내고, 그 알림이
     * 다시 이 메서드를 부르는 식으로 이어진다. 여기서 끝내면 다음 알림 때는 답할 사람이 없어
     * 조용히 끝난다.
     *
     * <p>여기에 {@code @Transactional} 을 붙이지 않는다. 예약된 스레드에서 자기 자신을 부르는
     * 경로라 프록시를 타지 않아서 붙여도 걸리지 않고, 읽기는 조회마다 알아서 트랜잭션이 열린다.
     * 실제 쓰기는 {@code ExchangeService.updateTimeSlots} 안에서 트랜잭션으로 묶인다.
     */
    void respond(Long exchangeId) {
        List<ExchangeParticipant> participants = participantRepository.findAllByExchangeId(exchangeId);

        Set<UUID> answered = timeSlotRepository.findAllByExchangeId(exchangeId).stream()
                .map(slot -> slot.getUser().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean humanAnswered = participants.stream()
                .map(participant -> participant.getUser().getId())
                .anyMatch(userId -> answered.contains(userId) && !DemoUser.isDemo(userId));

        if (!humanAnswered) {
            return;
        }

        List<Integer> humanSlots = humanSlots(exchangeId);

        for (ExchangeParticipant participant : participants) {
            UUID userId = participant.getUser().getId();

            if (answered.contains(userId) || !DemoUser.isDemo(userId)) {
                continue;
            }

            exchangeService.updateTimeSlots(exchangeId, userId, slotsFor(userId, humanSlots));
        }
    }

    /** 사람이 고른 칸. 더미가 여기에 맞춰 답해야 약속이 잡히는 흐름을 볼 수 있다. */
    private List<Integer> humanSlots(Long exchangeId) {
        return timeSlotRepository.findAllByExchangeId(exchangeId).stream()
                .filter(slot -> !DemoUser.isDemo(slot.getUser().getId()))
                .map(ExchangeTimeSlot::getSlotIndex)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * 더미가 고를 칸을 정한다.
     *
     * <p>셋 중 하나는 늦은 시간만 고른다. 겹치는 칸이 없어서 "맞는 시간이 없어요" 로 가는 흐름도
     * 시연할 수 있어야 하기 때문이다. 무작위가 아니라 UUID 로 정하는 것은, 같은 상대를 다시
     * 만났을 때 결과가 바뀌면 재현이 안 되기 때문이다.
     */
    private List<Integer> slotsFor(UUID userId, List<Integer> humanSlots) {
        boolean prefersLate = Math.floorMod(userId.hashCode(), 3) == 2;

        if (prefersLate || humanSlots.isEmpty()) {
            return List.of(TimeSlotGrid.SLOT_COUNT - 3, TimeSlotGrid.SLOT_COUNT - 2, TimeSlotGrid.SLOT_COUNT - 1);
        }

        int first = humanSlots.getFirst();
        List<Integer> slots = new ArrayList<>(
                List.of(first, Math.min(first + 1, TimeSlotGrid.SLOT_COUNT - 1)));

        if (!slots.contains(TimeSlotGrid.SLOT_COUNT / 2)) {
            slots.add(TimeSlotGrid.SLOT_COUNT / 2);
        }

        return slots.stream().distinct().sorted().toList();
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
