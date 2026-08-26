package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.BoothView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ZoneView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
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
    private final ExchangeRepository exchangeRepository;
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

    public List<ItemView> findItems(Long boothId) {
        return itemRepository.findByBoothIdOrderByIdAsc(boothId).stream().map(ItemView::of).toList();
    }

    /** 카드 고르기 화면이 부스와 상관없이 전체 목록을 필요로 한다. */
    public List<ItemView> findAllItems() {
        return itemRepository.findAll().stream().map(ItemView::of).toList();
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
     * 구역을 지운다.
     *
     * <p><b>약속이 걸려 있으면 먼저 막는다.</b> 그냥 지우면 FK 제약에 걸려 500 이 나가는데,
     * 운영자가 받는 것은 이유가 안 적힌 오류 화면이라 무엇을 정리해야 하는지 알 수 없다.
     * 끝난 약속도 그 자리를 기록으로 들고 있어서 여기에 걸린다. 교환을 먼저 정리하고 지운다.
     */
    @Transactional
    public void deleteZone(Long zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ApplicationException(ErrorCode.ZONE_NOT_FOUND);
        }

        if (exchangeRepository.existsByZoneId(zoneId)) {
            throw new ApplicationException(ErrorCode.ZONE_IN_USE);
        }

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

    @Transactional
    public void deleteItem(Long itemId) {
        itemRepository.deleteById(itemId);
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
