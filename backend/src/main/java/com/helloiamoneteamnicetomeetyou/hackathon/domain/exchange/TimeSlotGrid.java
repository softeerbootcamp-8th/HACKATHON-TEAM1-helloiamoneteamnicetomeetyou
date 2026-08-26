package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange;

import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 시간 선택 격자의 규칙이다. 15분 간격 8칸으로 지금부터 2시간을 덮는다.
 *
 * <p><b>여기 있는 두 상수는 프론트의 {@code store/time.ts} 와 같은 값이어야 한다.</b> 한쪽만
 * 고치면 화면에 그려진 칸 수와 서버가 받아 주는 칸 수가 어긋나서, 마지막 칸을 누른 사람만
 * 400 을 받는 식으로 조용히 깨진다.
 *
 * <p>격자의 시작점은 서버가 교환을 만들 때 한 번 정해서 {@code Exchange.slotBaseTime} 에 넣는다.
 * <b>클라이언트가 각자 자기 시계로 격자를 만들면 안 된다.</b> 14:03 에 화면을 연 사람의 0번 칸은
 * 14:15 이고 14:20 에 연 사람의 0번 칸은 14:30 이라, 같은 번호가 서로 다른 시각을 뜻하게 된다.
 */
public final class TimeSlotGrid {

    public static final int SLOT_COUNT = 8;
    public static final int SLOT_MINUTES = 15;

    private TimeSlotGrid() {
    }

    /** 격자의 시작점. 15분 단위로 올려서 이미 지나간 칸이 생기지 않게 한다. */
    public static LocalDateTime baseTimeFrom(LocalDateTime now) {
        LocalDateTime onMinute = now.withSecond(0).withNano(0);
        int remainder = onMinute.getMinute() % SLOT_MINUTES;

        return remainder == 0 ? onMinute : onMinute.plusMinutes(SLOT_MINUTES - remainder);
    }

    /** 칸 번호를 실제 시각으로 바꾼다. */
    public static LocalDateTime timeOf(LocalDateTime baseTime, int slotIndex) {
        validate(slotIndex);

        return baseTime.plusMinutes((long) slotIndex * SLOT_MINUTES);
    }

    public static void validateAll(Collection<Integer> slotIndexes) {
        slotIndexes.forEach(TimeSlotGrid::validate);
    }

    private static void validate(Integer slotIndex) {
        if (slotIndex == null || slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            throw new ApplicationException(ErrorCode.INVALID_TIME_SLOT);
        }
    }

    /**
     * 모두가 고른 칸 중 가장 빠른 것을 찾는다. 없으면 {@code null} 이다.
     *
     * <p>아무도 고르지 않은 상태에서 "전원 일치" 가 되어 버리면 안 되기 때문에, 빈 줄이 하나라도
     * 있으면 겹치는 칸이 없는 것으로 본다. 부르는 쪽에서 걸러 주더라도 여기서 한 번 더 막는다.
     */
    public static Integer earliestOverlap(Collection<List<Integer>> rows) {
        if (rows.isEmpty() || rows.stream().anyMatch(List::isEmpty)) {
            return null;
        }

        for (int index = 0; index < SLOT_COUNT; index++) {
            int slotIndex = index;
            if (rows.stream().allMatch(row -> row.contains(slotIndex))) {
                return slotIndex;
            }
        }

        return null;
    }
}
