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
 * <p>서버가 뜰 때마다 도는 코드라 이미 있는 것은 건드리지 않는다. <b>다만 "부스가 있으면 전부
 * 건너뛴다" 처럼 뭉뚱그려 보면 안 된다.</b> 부스만 있고 구역이 없는 DB 를 만나면 아무것도 넣지
 * 않고 지나가서, 교환을 만들 때 ZONE_NOT_FOUND 로 막힌다. 팀원이 손으로 넣어 본 흔적이 남아
 * 있거나 스키마를 다시 만든 DB 에서 실제로 겪는 일이라, 종류마다 따로 본다.
 *
 * <p>{@code data.sql} 대신 코드로 넣는 것은 {@code ddl-auto: update} 와 함께 쓸 때 실행 순서가
 * 헷갈리지 않게 하려는 것이다. 마이그레이션 도구가 들어오면 이 클래스는 그쪽으로 옮긴다.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class InitialDataSeeder implements ApplicationRunner {

    /**
     * 교환 장소와 약도 위 자리다. 뒤의 두 숫자가 약도 너비·높이에 대한 백분율이다.
     *
     * <p>행사장 약도 이미지가 아직 없어서 자리는 우리가 임의로 정했다. 다만 그 값이 화면에
     * 박혀 있으면 구역을 늘릴 때마다 프론트를 고쳐야 해서, DB 에 넣고 응답으로 내려보낸다.
     */
    private static final List<Object[]> ZONES = List.of(
            new Object[]{"중앙 포토존 앞", "행사 중앙 포토존", 52, 44},
            new Object[]{"에스컬레이터", "1층 에스컬레이터 앞", 25, 68},
            new Object[]{"라운지", "휴게 라운지", 82, 60});

    /**
     * 프론트 목업의 카드와 <b>이름이 같아야 한다.</b> 화면이 목업 카드와 서버 카드를 이름으로
     * 잇기 때문에(`features/catalog/match-by-name.ts`), 하나라도 어긋나면 그 카드는 등록이 안 된다.
     */
    private static final List<String[]> ITEMS = List.of(
            new String[]{"IONIQ 5 N", "아이오닉 5 N"},
            new String[]{"AVANTE N", "아반떼 N"},
            new String[]{"VELOSTER N", "벨로스터 N"},
            new String[]{"KONA N", "코나 N"},
            new String[]{"i30 N", "i30 N"},
            new String[]{"i30 Fastback", "i30 패스트백"},
            new String[]{"i20 N", "i20 N"},
            new String[]{"AVANTE N Facelift", "아반떼 N 페이스리프트"},
            new String[]{"i20 N Rally1", "i20 N 랠리1"});

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Booth booth = boothRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> boothRepository.save(Booth.of("현대자동차 팝업", "자동차 포토카드 교환")));

        List<Zone> existing = zoneRepository.findByBoothIdOrderByIdAsc(booth.getId());

        int zones = 0;
        if (existing.isEmpty()) {
            ZONES.forEach(zone -> zoneRepository.save(
                    Zone.of(booth, (String) zone[0], (String) zone[1], (int) zone[2], (int) zone[3])));
            zones = ZONES.size();
        } else {
            /*
              이미 구역이 있는 DB 다. 약도 자리 컬럼이 나중에 생겨서 전부 기본값(50, 50)으로
              들어가 있는데, 그러면 핀이 약도 한가운데 겹쳐 뜬다. 우리가 정해 둔 자리로 채워 준다.
              어드민에서 옮긴 자리를 되돌리지 않도록 기본값인 것만 건드린다.
            */
            for (int i = 0; i < existing.size() && i < ZONES.size(); i++) {
                Zone zone = existing.get(i);
                if (zone.getMapX() == 50 && zone.getMapY() == 50) {
                    zone.moveOnMap((int) ZONES.get(i)[2], (int) ZONES.get(i)[3]);
                }
            }
        }

        int items = 0;
        if (itemRepository.count() == 0) {
            ITEMS.forEach(item -> itemRepository.save(Item.of(booth, item[0], item[1])));
            items = ITEMS.size();
        }

        if (zones > 0 || items > 0) {
            log.info("행사 데이터를 넣었다: booth={}, zone={}개, item={}개", booth.getId(), zones, items);
        }
    }
}
