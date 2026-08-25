package com.helloiamoneteamnicetomeetyou.hackathon.global.seed;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 데이터를 넣는다. 부스 하나, 그 안의 교환 장소 셋, 포토카드 일곱 종이다.
 *
 * <p><b>부스가 이미 있으면 아무것도 하지 않는다.</b> 서버가 뜰 때마다 도는 코드라, 이 가드가
 * 없으면 배포할 때마다 같은 부스가 하나씩 늘어난다.
 *
 * <p>{@code data.sql} 대신 코드로 넣는 것은 {@code ddl-auto: update} 와 함께 쓸 때 실행 순서가
 * 헷갈리지 않게 하려는 것이다. 마이그레이션 도구가 들어오면 이 클래스는 그쪽으로 옮긴다.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class InitialDataSeeder implements ApplicationRunner {

    /** 화면의 핀 배치가 이 순서에 기댄다. 순서를 바꾸면 프론트의 좌표표도 같이 고친다. */
    private static final List<String[]> ZONES = List.of(
            new String[]{"중앙 포토존 앞", "행사 중앙 포토존"},
            new String[]{"에스컬레이터", "1층 에스컬레이터 앞"},
            new String[]{"라운지", "휴게 라운지"});

    private static final List<String[]> ITEMS = List.of(
            new String[]{"N Vision 74", "N 비전 74"},
            new String[]{"IONIQ 5 N", "아이오닉 5 N"},
            new String[]{"PONY", "포니"},
            new String[]{"AVANTE N", "아반떼 N"},
            new String[]{"GRANDEUR", "그랜저"},
            new String[]{"SANTA FE", "싼타페"},
            new String[]{"CASPER", "캐스퍼"});

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (boothRepository.count() > 0) {
            return;
        }

        Booth booth = boothRepository.save(Booth.of("현대자동차 팝업", "자동차 포토카드 교환"));

        ZONES.forEach(zone -> zoneRepository.save(Zone.of(booth, zone[0], zone[1])));
        ITEMS.forEach(item -> itemRepository.save(Item.of(booth, item[0], item[1])));

        log.info("행사 데이터를 넣었다: booth={}, zone={}개, item={}개",
                booth.getId(), ZONES.size(), ITEMS.size());
    }
}
