package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 뜰 때 {@code user_have_items.version} 의 빈 칸을 0 으로 채운다.
 *
 * <p><b>NULL 인 줄은 그때부터 아무것도 못 한다.</b> {@code @Version} 을 붙이기 전에 들어간 줄은
 * 컬럼이 NULL 인데({@code ddl-auto: update} 는 이미 있는 줄을 채워 주지 않는다), JPA 는
 * {@code where id = ? and version = ?} 로 고치기 때문에 NULL 과는 절대 맞지 않는다. 그래서
 * 매칭이 그 카드를 예약하려 들 때마다 {@code expected row count 1 but was 0} 으로 죽었고, 그
 * 사람은 카드를 다시 등록하기 전까지 영영 매칭이 되지 않았다. 배포 로그에 쌓여 있던
 * {@code ObjectOptimisticLockingFailureException} 이 이것이다.
 *
 * <p>해커톤 기간에는 마이그레이션 도구를 넣지 않기로 해서 부팅 때 한 번 도는 자리로 뒀다. 채울
 * 것이 없으면 아무 일도 하지 않으므로 다시 떠도 안전하다. 컬럼에서 NULL 이 사라지고 나면 이
 * 클래스는 지워도 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HaveItemVersionBackfill implements ApplicationRunner {

    private final UserHaveItemRepository userHaveItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int filled = userHaveItemRepository.initializeNullVersions();
        if (filled > 0) {
            log.warn("내놓은 카드 {}줄의 버전이 비어 있어 0 으로 채웠다. 그 줄들은 그동안 매칭에서 계속 밀려났다.", filled);
        }
    }
}
