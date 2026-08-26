package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;

/** 부스 목록 한 줄. 구역과 카드가 몇 개인지가 같이 보여야 어느 부스가 준비된 것인지 안다. */
public record BoothView(Long id, String name, String description, long zoneCount, long itemCount, int connectedCount) {

    public static BoothView of(Booth booth, long zoneCount, long itemCount, int connectedCount) {
        return new BoothView(booth.getId(), booth.getName(), booth.getDescription(), zoneCount, itemCount, connectedCount);
    }
}
