package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

/**
 * 카드 하나의 수요와 공급.
 *
 * <p>{@code holders} 는 그 카드를 내놓을 수 있는 사람 수이고 {@code seekers} 는 찾는 사람
 * 수다. 매칭이 붙으려면 두 값이 모두 있어야 하는데, 한쪽이 0 이면 그 카드는 아무리 기다려도
 * 짝이 생기지 않는다. 부스에서 더미를 어디에 넣을지 이 표를 보고 정한다.
 */
public record ItemDemandView(ItemView item, long holders, long seekers) {

    /** 짝이 날 수 없는 카드. 화면에서 눈에 띄게 표시한다. */
    public boolean isDeadEnd() {
        return holders == 0 || seekers == 0;
    }

    /** 찾는 사람이 내놓는 사람보다 많은 카드. 이 카드를 가진 더미를 넣으면 바로 매칭이 붙는다. */
    public boolean isShort() {
        return seekers > holders;
    }
}
