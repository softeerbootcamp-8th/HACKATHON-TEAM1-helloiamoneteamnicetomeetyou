package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity;

public enum ItemStatus {
    LEFT,      // 미매칭, 교환 가능
    RESERVED,  // 매칭됨, 교환 확정 대기
    OUT        // 교환 완료
}
