package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.enums;

public enum ParticipantStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    /** 약속 장소에 도착했다고 누른 상태. 상대 화면의 "도착" 배지가 이걸 본다. */
    ARRIVED
}
