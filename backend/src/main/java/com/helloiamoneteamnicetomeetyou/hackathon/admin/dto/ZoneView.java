package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;

/**
 * 부스 안의 구역 하나.
 *
 * <p>{@code mapX}, {@code mapY} 는 약도 위 자리다. 약도 너비와 높이에 대한 백분율(0~100)이라
 * 거리 계산에 쓰는 값이 아니다. 어드민에서 이 값을 고칠 수 있어야 만든 구역이 약도 한가운데에
 * 겹쳐 뜨지 않는다.
 */
public record ZoneView(Long id, String name, String location, int mapX, int mapY) {

    public static ZoneView of(Zone zone) {
        return new ZoneView(zone.getId(), zone.getName(), zone.getLocation(), zone.getMapX(), zone.getMapY());
    }
}
