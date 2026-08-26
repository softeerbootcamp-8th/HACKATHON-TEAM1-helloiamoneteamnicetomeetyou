package com.helloiamoneteamnicetomeetyou.hackathon.global.seed;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 데이터를 넣는다. 부스 하나, 그 안의 지정 교환장소 하나, 포토카드 일곱 종이다.
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
     * 지정 교환장소와 약도 위 자리다. 뒤의 두 숫자가 약도 너비·높이에 대한 백분율이다.
     *
     * <p><b>한 곳만 넣는다.</b> 만나는 자리는 팝업 운영자가 정해 둔 한 곳이고, 사용자 화면은
     * 그 자리를 고르는 것이 아니라 확인한다. 부스의 첫 구역이 그 자리가 된다.
     *
     * <p>행사장 약도 이미지가 아직 없어서 자리는 우리가 임의로 정했다. 다만 그 값이 화면에
     * 박혀 있으면 자리를 옮길 때마다 프론트를 고쳐야 해서, DB 에 넣고 응답으로 내려보낸다.
     */
    // 원소가 하나라 List.of 가 배열을 가변인자로 펼친다. 타입을 못 박아 그걸 막는다.
    private static final List<Object[]> ZONES = List.<Object[]>of(
            new Object[]{"중앙 포토존 앞", "행사 중앙 포토존", 52, 44});

    /** 카드 그림이 올라가 있는 스토리지. 파일 이름은 영문 카드 이름을 밑줄로 이은 것이다. */
    private static final String IMAGE_BASE =
            "https://sdumqvkniemiowanvsef.supabase.co/storage/v1/object/public/items";

    /**
     * 이 부스가 내놓는 카드다. 순서대로 영문 이름, 한글 이름, 그림 파일 이름이다.
     *
     * <p>한글 이름은 {@code description} 자리에 들어간다. 화면이 카드 밑에 작게 붙여 쓰는
     * 값이라 따로 컬럼을 두지 않고 설명을 그대로 쓴다.
     *
     * <p><b>그림 주소를 여기서 채워야 한다.</b> 화면은 이제 서버가 준 {@code imageUrl} 만
     * 보고 카드를 그린다. 비어 있으면 그림 없이 약칭 글자만 뜬다.
     */
    private static final List<String[]> ITEMS = List.of(
            new String[]{"IONIQ 5 N", "아이오닉 5 N", "IONIQ5_N.png"},
            new String[]{"AVANTE N", "아반떼 N", "AVANTE_N.png"},
            new String[]{"VELOSTER N", "벨로스터 N", "VELOSTER_N.png"},
            new String[]{"KONA N", "코나 N", "KONA_N.png"},
            new String[]{"i30 N", "i30 N", "i30_N.png"},
            new String[]{"i30 Fastback", "i30 패스트백", "i30_Fastback.png"},
            new String[]{"i20 N", "i20 N", "i20_N.png"},
            // 파일 이름에 공백이 들어 있어 그대로 두면 주소가 끊긴다.
            new String[]{"AVANTE N Facelift", "아반떼 N 페이스리프트", "AVANTE_N%20Facelift.png"},
            new String[]{"i20 N Rally1", "i20 N 랠리1", "i20_N_Rally1.png"});

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
            ITEMS.forEach(item -> itemRepository.save(
                    Item.of(booth, item[0], item[1], IMAGE_BASE + "/" + item[2])));
            items = ITEMS.size();
        } else {
            /*
              이미 카드가 들어 있는 DB 다. 그림 주소는 나중에 넣기 시작한 값이라 먼저 만들어진
              카드는 전부 비어 있는데, 화면이 이제 서버 imageUrl 만 보고 그리기 때문에 그대로
              두면 카드가 전부 약칭 글자로만 뜬다. 이름이 같은 카드의 빈 자리만 채운다.
              운영자가 어드민에서 넣은 주소는 건드리지 않는다.
            */
            Map<String, String> imageByName = ITEMS.stream()
                    .collect(Collectors.toMap(item -> item[0], item -> IMAGE_BASE + "/" + item[2]));

            itemRepository.findByBoothIdOrderByIdAsc(booth.getId()).stream()
                    .filter(item -> item.getImageUrl() == null)
                    .forEach(item -> {
                        String url = imageByName.get(item.getName());
                        if (url != null) {
                            item.attachImage(url);
                        }
                    });
        }

        if (zones > 0 || items > 0) {
            log.info("행사 데이터를 넣었다: booth={}, zone={}개, item={}개", booth.getId(), zones, items);
        }
    }
}
