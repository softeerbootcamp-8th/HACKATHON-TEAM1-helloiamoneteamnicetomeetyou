package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ExchangeView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ParticipantView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.entity.ExchangeParticipant;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.List;
import java.util.Map;
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
 * <p>시간 슬롯 선택과 약속 확정은 아직 없다. 이슈 #29 의 {@code ExchangeService} 가 dev 에
 * 들어오면 그 서비스를 그대로 부르면 되고, 그때까지 여기서 격자 계산을 다시 만들지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;
    private final SseEventPublisher sseEventPublisher;

    public List<ExchangeView> findExchanges() {
        List<Exchange> exchanges = exchangeRepository.findAllForAdmin();
        if (exchanges.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ExchangeParticipant>> participantsByExchange =
                exchangeParticipantRepository
                        .findAllByExchangeIdIn(exchanges.stream().map(Exchange::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(participant -> participant.getExchange().getId()));

        return exchanges.stream()
                .map(exchange -> toView(exchange, participantsByExchange.getOrDefault(exchange.getId(), List.of())))
                .toList();
    }

    /** 오른쪽 패널에서 "이 사람이 낀 약속" 을 보여 줄 때 쓴다. */
    public List<ExchangeView> findExchangesOf(java.util.UUID userId) {
        return findExchanges().stream()
                .filter(view -> view.participants().stream().anyMatch(p -> p.userId().equals(userId)))
                .toList();
    }

    private ExchangeView toView(Exchange exchange, List<ExchangeParticipant> participants) {
        return new ExchangeView(
                exchange.getId(),
                exchange.getStatus(),
                exchange.getType().name(),
                exchange.getZone() == null ? null : exchange.getZone().getName(),
                exchange.getZone() == null ? null : exchange.getZone().getBooth().getName(),
                exchange.getExchangeTime(),
                exchange.getCreatedAt(),
                participants.stream().map(this::toParticipantView).toList());
    }

    private ParticipantView toParticipantView(ExchangeParticipant participant) {
        User user = participant.getUser();
        return new ParticipantView(
                participant.getId(),
                user.getId(),
                user.getId().toString().substring(0, 8),
                user.getUsername(),
                participant.getStatus(),
                user.isAdminManaged());
    }

    /** 더미 대신 수락한다. 참가자 전원의 화면이 바뀌어야 해서 SSE 를 같이 내보낸다. */
    @Transactional
    public void acceptAsDummy(Long participantId) {
        ExchangeParticipant participant = findDummyParticipant(participantId);
        participant.accept();
        publishToBooth(participant.getExchange(), SseEventType.MATCH_ACCEPTED);
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
