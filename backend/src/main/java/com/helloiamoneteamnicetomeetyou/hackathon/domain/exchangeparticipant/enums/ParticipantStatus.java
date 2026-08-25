package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.enums;

public enum ParticipantStatus {
    PENDING("답변 기다리는 중"),
    ACCEPTED("수락"),
    REJECTED("거절");

    private final String label;

    ParticipantStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
