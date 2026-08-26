package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service.MatchingService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("매칭 트리거 모으기")
class MatchTriggerEventListenerTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final long DEBOUNCE_MS = 200;

    @Mock
    private MatchingService matchingService;

    private ThreadPoolTaskScheduler scheduler;
    private MatchTriggerEventListener listener;

    @BeforeEach
    void 리스너를_만든다() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.initialize();

        listener = new MatchTriggerEventListener(matchingService, scheduler);
        ReflectionTestUtils.setField(listener, "debounceMs", DEBOUNCE_MS);
    }

    @AfterEach
    void 스케줄러를_닫는다() {
        scheduler.shutdown();
    }

    @Test
    @DisplayName("한 사람의 트리거가 몰려 오면 마지막 것으로 한 번만 돌린다")
    void 몰려_오면_한_번만_돌린다() {
        // 등록 화면이 고른 카드를 한 장에 한 번씩 동시에 보내서 실제로 이렇게 들어온다.
        for (int i = 0; i < 7; i++) {
            listener.onMatchTrigger(new MatchTriggerEvent(USER));
        }

        verify(matchingService, timeout(2000).times(1)).runMatching(USER);
    }

    @Test
    @DisplayName("사람이 다르면 각각 돌린다")
    void 사람이_다르면_각각_돌린다() {
        listener.onMatchTrigger(new MatchTriggerEvent(USER));
        listener.onMatchTrigger(new MatchTriggerEvent(OTHER));

        verify(matchingService, timeout(2000)).runMatching(USER);
        verify(matchingService, timeout(2000)).runMatching(OTHER);
    }

    @Test
    @DisplayName("모으는 시간이 0 이면 예약하지 않고 곧바로 돌린다")
    void 영이면_곧바로_돌린다() {
        ReflectionTestUtils.setField(listener, "debounceMs", 0L);

        listener.onMatchTrigger(new MatchTriggerEvent(USER));
        listener.onMatchTrigger(new MatchTriggerEvent(USER));

        verify(matchingService, times(2)).runMatching(USER);
    }

    @Test
    @DisplayName("모으는 동안에는 아직 돌지 않는다")
    void 모으는_동안에는_돌지_않는다() {
        listener.onMatchTrigger(new MatchTriggerEvent(USER));

        // 모으는 시간의 절반이 지난 시점에는 아직 예약이 안 터졌다.
        verify(matchingService, after(DEBOUNCE_MS / 2).never()).runMatching(USER);
    }
}
