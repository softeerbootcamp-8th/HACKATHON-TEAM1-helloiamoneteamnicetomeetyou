package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;

/**
 * 교환 장소 하나.
 *
 * <p>{@code mapX}, {@code mapY} 는 약도 위 자리다. 약도 너비와 높이에 대한 백분율(0~100)이고,
 * 거리 계산에 쓰는 좌표가 아니라 그림 위에 핀을 찍을 비율이다.
 *
 * <p>약도 이미지 자체는 서버가 주지 않는다. 행사장 약도 자산이 아직 없어서 화면이 격자로 대신
 * 그리고 있고, 이미지가 들어오면 그때 이 응답에 더한다.
 */
public record ZoneResponseDto(Long id, String name, String location, int mapX, int mapY) {

    public static ZoneResponseDto from(Zone zone) {
        return new ZoneResponseDto(
                zone.getId(), zone.getName(), zone.getLocation(), zone.getMapX(), zone.getMapY());
    }
}
