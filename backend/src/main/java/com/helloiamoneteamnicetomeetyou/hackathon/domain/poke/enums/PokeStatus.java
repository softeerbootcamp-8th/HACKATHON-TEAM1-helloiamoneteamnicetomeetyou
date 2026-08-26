package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums;

/**
 * 찔러보기 한 건의 상태다.
 *
 * <p>{@code ParticipantStatus} 와 값이 같지만 따로 둔다. 저쪽은 성사된 교환에 참가한 사람의
 * 상태이고, 이쪽은 아직 교환이 되기 전 제안의 상태다. 같은 이름을 공유하면 찔러보기가
 * 수락됐을 때 어느 쪽 상태를 바꿔야 하는지 읽는 사람이 헷갈린다.
 */
public enum PokeStatus {

    /** 보냈고 상대의 답을 기다린다. 같은 상대에게 이 상태가 있으면 재신청을 막는다. */
    PENDING,

    /** 상대가 내 카드 묶음에서 한 장을 골랐다. 이때 교환이 만들어진다. */
    ACCEPTED,

    REJECTED
}
