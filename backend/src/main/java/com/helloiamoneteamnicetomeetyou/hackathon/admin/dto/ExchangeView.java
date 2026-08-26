package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.TimeSlotGrid;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

public record ExchangeView(
        Long id,
        ExchangeStatus status,
        String type,
        String zoneName,
        String boothName,
        LocalDateTime exchangeTime,
        LocalDateTime createdAt,
        LocalDateTime slotBaseTime,
        Integer overlapSlot,
        List<ParticipantView> participants) {

    private static final DateTimeFormatter SLOT_LABEL = DateTimeFormatter.ofPattern("HH:mm");

    public String statusLabel() {
        return status.getLabel();
    }

    /** 하나라도 더미가 끼어 있으면 어드민이 대신 눌러 줘야 하는 교환이다. */
    public boolean hasDummy() {
        return participants.stream().anyMatch(ParticipantView::dummy);
    }

    /**
     * 시간 격자를 보여 줄 때인지.
     *
     * <p>시각이 확정된 뒤에는 서비스가 {@code EXCHANGE_TIME_ALREADY_CONFIRMED} 로 거절한다.
     * 눌러 봐야 안 되는 격자를 띄워 두면 운영자가 무엇이 잘못됐는지 화면에서 알 수 없다.
     * 끝나거나 취소된 교환도 마찬가지다.
     */
    /** 만날 시각이 정해졌는지. 도착 표시는 이 뒤에만 할 수 있다. */
    public boolean timeConfirmed() {
        return exchangeTime != null;
    }

    public boolean timeGridOpen() {
        return slotBaseTime != null
                && exchangeTime == null
                && (status == ExchangeStatus.PENDING || status == ExchangeStatus.IN_PROGRESS);
    }

    /**
     * 머리 줄에 적을 시각. 사용자 화면(`screens/TimeSelect.tsx`)이 그리는 것과 같은 격자다.
     *
     * <p>칸 번호는 이 목록의 순번이다. 실제 시각은 격자 시작점과 함께 봐야 나오기 때문에,
     * 어드민에서 계산을 다시 만들지 않고 {@link TimeSlotGrid} 를 그대로 쓴다.
     */
    public List<String> slotLabels() {
        if (slotBaseTime == null) {
            return List.of();
        }

        return IntStream.range(0, TimeSlotGrid.SLOT_COUNT)
                .mapToObj(index -> TimeSlotGrid.timeOf(slotBaseTime, index).format(SLOT_LABEL))
                .toList();
    }
}
