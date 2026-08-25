package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.demo;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대기장소에 세워 둘 더미 사용자와 그들이 가진/찾는 카드를 넣는다.
 *
 * <p>화면의 매칭이 아직 목업이라 상대가 DB 에 없으면 교환을 만들 수 없다. 여기서 넣는 사용자들이
 * 실제 {@code users} 행이 되고, 그 사람이 고른 시간도 진짜로 {@code exchange_time_slots} 에
 * 들어간다. 흉내내는 것은 상대가 언제 응답하느냐뿐이다.
 *
 * <p>가진 카드와 찾는 카드는 지금 화면이 쓰지 않는다. 매칭 알고리즘(이슈 #20)이 붙었을 때 바로
 * 돌려볼 데이터가 있게 미리 넣어 둔다.
 *
 * <p>{@code @Order} 로 행사 데이터 뒤에 돈다. 아이템이 있어야 가진 카드를 걸 수 있다.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    /** {key, 내놓은 카드 index, 찾는 카드 index 들} — 프론트 {@code mocks/data.ts} 와 같은 구성이다. */
    private static final List<int[]> HOLDINGS = List.of(
            new int[]{6, 0, 1},   // 캐스퍼: CASPER 를 내놓고 N74, I5N 을 찾는다
            new int[]{0, 5, 2},   // 블루N
            new int[]{1, 2, 0},   // 아이오닉러버
            new int[]{3, 5},      // N드라이버
            new int[]{4, 2, 6},   // 그랜저러버
            new int[]{2, 0, 3},   // 포니덕후
            new int[]{1, 2, 5},   // 레몬 16
            new int[]{2, 0, 4},   // 레몬 07
            new int[]{5, 3, 1},   // 싼타페러버
            new int[]{0, 6});     // 비전러버

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Item> items = itemRepository.findAll();

        if (items.isEmpty()) {
            log.warn("아이템이 없어 더미 사용자를 넣지 않는다");
            return;
        }

        int created = 0;

        for (int i = 0; i < DemoUser.ALL.size(); i++) {
            DemoUser demo = DemoUser.ALL.get(i);

            if (userRepository.existsById(demo.id())) {
                continue;
            }

            User user = userRepository.save(User.of(demo.id(), demo.username()));
            int[] holding = HOLDINGS.get(i);

            userHaveItemRepository.save(UserHaveItem.of(user, items.get(holding[0]), 1));
            for (int w = 1; w < holding.length; w++) {
                userWantItemRepository.save(UserWantItem.of(user, items.get(holding[w])));
            }

            created++;
        }

        if (created > 0) {
            log.info("더미 사용자 {}명을 넣었다", created);
        }
    }
}
