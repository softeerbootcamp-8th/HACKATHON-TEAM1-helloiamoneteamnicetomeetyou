package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums;

public enum ExchangeStatus {
    PENDING("시간 고르는 중"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    /**
     * 화면에 쓰는 이름이다.
     *
     * <p>여기 두지 않으면 상태가 늘 때마다 화면 여기저기의 조건문을 같이 고쳐야 하고, 한 군데를
     * 빼먹으면 그 상태만 영어로 나온다.
     */
    private final String label;

    ExchangeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
