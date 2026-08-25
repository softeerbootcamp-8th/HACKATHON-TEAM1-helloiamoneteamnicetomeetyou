package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.entity.Zone;

public record ZoneView(Long id, String name, String location) {

    public static ZoneView of(Zone zone) {
        return new ZoneView(zone.getId(), zone.getName(), zone.getLocation());
    }
}
