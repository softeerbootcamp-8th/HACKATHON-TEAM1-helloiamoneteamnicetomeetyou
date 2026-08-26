package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("시간 선택 격자")
class TimeSlotGridTest {

    @Test
    @DisplayName("격자 시작점은 15분 단위로 올린다")
    void 격자_시작점은_15분_단위로_올린다() {
        LocalDateTime base = TimeSlotGrid.baseTimeFrom(LocalDateTime.of(2026, 8, 25, 14, 3, 42));

        assertThat(base).isEqualTo(LocalDateTime.of(2026, 8, 25, 14, 15));
    }

    @Test
    @DisplayName("이미 15분 경계면 그대로 둔다")
    void 이미_15분_경계면_그대로_둔다() {
        LocalDateTime base = TimeSlotGrid.baseTimeFrom(LocalDateTime.of(2026, 8, 25, 14, 30, 0));

        assertThat(base).isEqualTo(LocalDateTime.of(2026, 8, 25, 14, 30));
    }

    @Test
    @DisplayName("칸 번호는 시작점에서 15분씩 떨어진 시각이 된다")
    void 칸_번호는_시각이_된다() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 25, 14, 15);

        assertThat(TimeSlotGrid.timeOf(base, 0)).isEqualTo(base);
        assertThat(TimeSlotGrid.timeOf(base, 3)).isEqualTo(LocalDateTime.of(2026, 8, 25, 15, 0));
    }

    @Test
    @DisplayName("격자 밖의 칸은 막는다")
    void 격자_밖의_칸은_막는다() {
        assertThatThrownBy(() -> TimeSlotGrid.validateAll(List.of(TimeSlotGrid.SLOT_COUNT)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_TIME_SLOT);

        assertThatThrownBy(() -> TimeSlotGrid.validateAll(List.of(-1)))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("모두가 고른 칸 중 가장 빠른 것을 고른다")
    void 겹치는_가장_빠른_칸을_고른다() {
        Integer overlap = TimeSlotGrid.earliestOverlap(
                List.of(List.of(0, 2, 3), List.of(2, 3, 5), List.of(3, 2)));

        assertThat(overlap).isEqualTo(2);
    }

    @Test
    @DisplayName("겹치는 칸이 없으면 null 이다")
    void 겹치는_칸이_없으면_null_이다() {
        assertThat(TimeSlotGrid.earliestOverlap(List.of(List.of(0, 1), List.of(5, 6)))).isNull();
    }

    @Test
    @DisplayName("아직 안 고른 사람이 있으면 겹친 것으로 보지 않는다")
    void 안_고른_사람이_있으면_겹치지_않은_것이다() {
        assertThat(TimeSlotGrid.earliestOverlap(List.of(List.of(0, 1), List.of()))).isNull();
        assertThat(TimeSlotGrid.earliestOverlap(List.of())).isNull();
    }
}
