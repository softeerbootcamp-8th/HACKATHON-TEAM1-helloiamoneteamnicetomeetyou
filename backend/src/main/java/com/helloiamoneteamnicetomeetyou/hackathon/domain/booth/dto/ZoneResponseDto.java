package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;

/**
 * 교환 장소 하나.
 *
 * <p>약도 위 좌표는 여기 없다. 행사장 약도 자체가 아직 목업이라 핀 자리는 화면이 정하고, 서버는
 * 어떤 자리들이 있고 무엇이라 부르는지만 알려 준다. 실제 약도가 들어오면 그때 좌표를 넣는다.
 */
public record ZoneResponseDto(Long id, String name, String location) {

    public static ZoneResponseDto from(Zone zone) {
        return new ZoneResponseDto(zone.getId(), zone.getName(), zone.getLocation());
    }
}
