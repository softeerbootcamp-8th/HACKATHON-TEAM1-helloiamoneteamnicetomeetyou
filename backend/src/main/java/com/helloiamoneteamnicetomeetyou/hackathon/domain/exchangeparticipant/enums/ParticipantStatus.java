package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.enums;

public enum ParticipantStatus {

    PENDING("답변 기다리는 중"),
    ACCEPTED("수락"),
    REJECTED("거절"),
    /** 약속 장소에 도착했다고 누른 상태. 상대 화면의 "도착" 배지가 이걸 본다. */
    ARRIVED("도착");

    private final String label;

    ParticipantStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
