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

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;
    private final ItemRepository itemRepository;
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

    @Transactional
    public void createZone(Long boothId, String name, String location) {
        zoneRepository.save(Zone.of(findBooth(boothId), name, location));
    }

    @Transactional
    public void updateZone(Long zoneId, String name, String location) {
        zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ZONE_NOT_FOUND))
                .update(name, location);
    }

    @Transactional
    public void deleteZone(Long zoneId) {
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
