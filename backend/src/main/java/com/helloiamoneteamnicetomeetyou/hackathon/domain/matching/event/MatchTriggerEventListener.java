package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service.MatchingService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@code AFTER_COMMIT} 인 이유: {@code runMatching} 이 비동기라, 커밋 전에 다른 스레드에서
 * 먼저 돌면 방금 반영한 카드 변경을 못 보고 지나친다. 실제로 반영된 뒤에 돌게 미룬다.
 *
 * <p><b>같은 사람의 이벤트가 몰려 오면 마지막 것만 돌린다.</b> 등록 화면은 고른 카드를 한 장에
 * 한 번씩, 그것도 {@code Promise.all} 로 동시에 보낸다(프론트 {@code use-register-selections.ts}).
 * 내놓을 카드 4장에 찾는 카드 3장이면 이벤트가 7개 나가고, 예전에는 매칭이 7번 돌았다.
 *
 * <p>낭비만 하는 것이 아니라 결과가 나빠진다. 찾는 카드 3장이 다 들어가기 전에 첫 장만 보고
 * 매칭이 성사되면 나머지 2장은 상대를 고르는 점수에 반영되지 못한다. 세 장을 다 봤으면 더 맞는
 * 상대가 있어도 놓치는 것이다.
 *
 * <p>그래서 이벤트가 올 때마다 그 사람의 예약을 취소하고 다시 잡는다. 마지막 이벤트로부터
 * {@code matching.trigger-debounce-ms} 만큼 조용하면 그때 한 번 돌린다. 등록이 모두 커밋된
 * 뒤라서 매칭이 전체 목록을 본다.
 *
 * <p>{@code 0} 이면 예약하지 않고 곧바로 돌린다. 시연 중에 이 지연이 문제가 되면 값 하나로
 * 예전 동작으로 되돌릴 수 있게 남겨 둔 길이다.
 */
@Component
@RequiredArgsConstructor
public class MatchTriggerEventListener {

    private final MatchingService matchingService;
    private final TaskScheduler taskScheduler;

    @Value("${matching.trigger-debounce-ms}")
    private long debounceMs;

    /**
     * 사용자별로 아직 안 터진 예약 하나.
     *
     * <p>돌고 난 예약을 지우지 않는다. 지우려면 "지금 이 자리에 있는 것이 내가 만든 예약이
     * 맞는가" 를 확인해야 하는데, 그 사이 새 이벤트가 예약을 갈아 끼우면 남의 것을 지운다.
     * 사람마다 한 칸이라 부스 규모에서는 그냥 두는 편이 간단하고 안전하다.
     */
    private final Map<UUID, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchTrigger(MatchTriggerEvent event) {
        UUID userId = event.userId();

        if (debounceMs <= 0) {
            matchingService.runMatching(userId);
            return;
        }

        pending.compute(userId, (id, scheduled) -> {
            // 아직 안 터진 예약이면 취소된다. 이미 터진 뒤라면 취소가 아무 일도 하지 않고,
            // 새로 잡은 예약이 그 자리를 덮는다.
            if (scheduled != null) {
                scheduled.cancel(false);
            }
            return taskScheduler.schedule(
                    () -> matchingService.runMatching(id),
                    Instant.now().plusMillis(debounceMs));
        });
    }
}
