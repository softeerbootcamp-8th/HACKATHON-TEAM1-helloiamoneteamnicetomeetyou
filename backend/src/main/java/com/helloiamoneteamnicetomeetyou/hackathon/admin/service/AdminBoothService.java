package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.BoothView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ZoneView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseConnectionManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 부스와 구역, 카드를 다룬다. 부스가 열리기 전에 채워 두는 값들이다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBoothService {

    /** 약도 한가운데. 자리를 안 정해 주면 여기 뜨고, 겹쳐 있는 것이 눈에 보인다. */
    private static final int DEFAULT_MAP_POSITION = 50;

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;
    private final ItemRepository itemRepository;
    private final AdminCleanupService adminCleanupService;
    private final SseConnectionManager sseConnectionManager;

    public List<BoothView> findBooths() {
        return boothRepository.findAllByOrderByIdAsc().stream()
                .map(booth -> BoothView.of(
                        booth,
                        zoneRepository.countByBoothId(booth.getId()),
                        itemRepository.countByBoothId(booth.getId()),
                        sseConnectionManager.countByBooth(booth.getId())))
                .toList();
    }

    public Booth findBooth(Long boothId) {
        return boothRepository.findById(boothId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.BOOTH_NOT_FOUND));
    }

    public List<ZoneView> findZones(Long boothId) {
        return zoneRepository.findByBoothIdOrderByIdAsc(boothId).stream().map(ZoneView::of).toList();
    }

    /** 부스를 가리지 않은 구역 전부. 교환의 만날 자리를 옮기는 드롭다운이 쓴다. */
    public List<ZoneView> findAllZones() {
        return zoneRepository.findAll().stream().map(ZoneView::of).toList();
    }

    public List<ItemView> findItems(Long boothId) {
        return itemRepository.findAllWithBoothByBoothId(boothId).stream().map(ItemView::of).toList();
    }

    /** 카드 고르기 화면이 부스와 상관없이 전체 목록을 필요로 한다. 부스 이름을 같이 보여 준다. */
    public List<ItemView> findAllItems() {
        return itemRepository.findAllWithBooth().stream().map(ItemView::of).toList();
    }

    @Transactional
    public Long createBooth(String name, String description) {
        return boothRepository.save(Booth.of(name, description)).getId();
    }

    @Transactional
    public void updateBooth(Long boothId, String name, String description) {
        findBooth(boothId).update(name, description);
    }

    /**
     * 부스를 통째로 지운다. 그 안의 카드와 구역까지 같이 사라진다.
     *
     * <p><b>마지막 남은 부스는 막는다.</b> 부스가 하나도 없으면 서비스 첫 화면이
     * "아직 열린 부스가 없습니다" 로 굳어서 아무도 아무것도 못 한다. 부스를 갈아 끼우려면 새
     * 부스를 먼저 만들고 지우면 된다.
     *
     * <p>카드는 {@link AdminCleanupService#deleteItemDeep} 로 딸린 것까지 걷어낸 뒤 지우고,
     * 구역은 약속에서 자리만 떼고 지운다. 카드 하나하나를 도는 것이 느려 보이지만 부스 하나에
     * 카드가 열 몇 장이고 부스를 지우는 일은 시연 준비 때 몇 번 있는 일이라, 삭제 규칙을 한 곳에
     * 두는 편이 낫다고 봤다.
     */
    @Transactional
    public BoothRemoval deleteBooth(Long boothId) {
        Booth booth = findBooth(boothId);
        if (boothRepository.count() <= 1) {
            throw new ApplicationException(ErrorCode.LAST_BOOTH);
        }

        List<Item> items = itemRepository.findByBoothIdOrderByIdAsc(boothId);
        int removedExchanges = 0;
        for (Item item : items) {
            removedExchanges += adminCleanupService.deleteItemDeep(item.getId());
        }
        itemRepository.deleteAll(items);

        List<Zone> zones = zoneRepository.findByBoothIdOrderByIdAsc(boothId);
        zones.forEach(zone -> adminCleanupService.detachZone(zone.getId()));
        zoneRepository.deleteAll(zones);

        boothRepository.delete(booth);

        return new BoothRemoval(items.size(), zones.size(), removedExchanges);
    }

    /** 부스를 지우면서 같이 사라진 것들. 화면이 운영자에게 그대로 읽어 준다. */
    public record BoothRemoval(int items, int zones, int exchanges) {}

    /**
     * 구역을 만든다. 약도 위 자리까지 여기서 받는다.
     *
     * <p><b>자리를 안 받으면 만든 구역이 전부 약도 한가운데에 겹쳐 뜬다.</b> 기본값이 50/50
     * 이라 두 번째 구역부터는 첫 번째 핀에 그대로 포개진다. 그러면 화면에서 고를 수가 없어서,
     * 어드민으로 구역을 늘려도 실제로는 못 쓰는 구역이 된다.
     */
    @Transactional
    public void createZone(Long boothId, String name, String location, Integer mapX, Integer mapY) {
        Zone zone = Zone.of(findBooth(boothId), name, location);
        zone.moveOnMap(mapX == null ? DEFAULT_MAP_POSITION : mapX, mapY == null ? DEFAULT_MAP_POSITION : mapY);

        zoneRepository.save(zone);
    }

    @Transactional
    public void updateZone(Long zoneId, String name, String location, Integer mapX, Integer mapY) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ZONE_NOT_FOUND));

        zone.update(name, location);
        if (mapX != null && mapY != null) {
            zone.moveOnMap(mapX, mapY);
        }
    }

    /**
     * 구역을 지운다. 여기서 만나기로 한 약속에서는 자리만 떼어 낸다.
     *
     * <p>예전에는 약속이 하나라도 걸려 있으면 막았다. 끝난 약속도 그 자리를 기록으로 들고 있어서,
     * 시연을 한 번 돌리고 나면 구역을 영영 못 지우게 됐다. 지금은 약속에서 자리를 비우고 지운다.
     * 진행 중이던 약속은 "장소 미정" 이 되고 교환 탭에서 다른 자리로 옮기면 된다.
     */
    @Transactional
    public void deleteZone(Long zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ApplicationException(ErrorCode.ZONE_NOT_FOUND);
        }

        adminCleanupService.detachZone(zoneId);
        zoneRepository.deleteById(zoneId);
    }

    @Transactional
    public void createItem(Long boothId, String name, String description, String imageUrl) {
        itemRepository.save(Item.of(findBooth(boothId), name, description, blankToNull(imageUrl)));
    }

    @Transactional
    public void updateItem(Long itemId, String name, String description, String imageUrl) {
        itemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND))
                .update(name, description, blankToNull(imageUrl));
    }

    /**
     * 카드를 지운다. 그 카드를 가리키는 것을 전부 걷어낸 뒤에 지운다.
     *
     * <p><b>그냥 {@code deleteById} 를 부르면 FK 제약에 걸려 500 이 나갔다.</b> 카드를 붙들고
     * 있는 표가 다섯(교환 기록, 찔러보기 두 자리, 내놓음, 찾음)인데, 운영자가 받는 것은 이유가
     * 안 적힌 오류 화면이라 무엇을 정리해야 하는지 알 수 없었다. 정리 순서는
     * {@link AdminCleanupService} 가 안다.
     *
     * @return 이 카드가 오가서 같이 지운 교환 건수. 화면이 운영자에게 알려 준다
     */
    @Transactional
    public int deleteItem(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ApplicationException(ErrorCode.ITEM_NOT_FOUND);
        }

        int removedExchanges = adminCleanupService.deleteItemDeep(itemId);
        itemRepository.deleteById(itemId);

        return removedExchanges;
    }

    /**
     * 빈 입력칸을 null 로 바꾼다.
     *
     * <p>폼은 비어 있어도 빈 문자열을 보낸다. 그대로 저장하면 화면이 "이미지가 있다" 로 보고
     * 깨진 이미지를 그리게 된다.
     */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
