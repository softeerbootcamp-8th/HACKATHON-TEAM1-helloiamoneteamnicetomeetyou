package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

/**
 * SSE 로 내려보내는 이벤트 이름을 한곳에 모아 둔다.
 *
 * <p>여기 있는 이름이 그대로 {@code event:} 필드로 나가고, 프론트는 {@code addEventListener} 로
 * 같은 문자열을 구독한다. 도메인마다 문자열을 직접 만들면 한쪽만 바꿔도 조용히 어긋나기 때문에,
 * 새 이벤트가 필요하면 이 enum 과 프론트의 {@code SseEventType} 을 같이 고친다.
 */
public enum SseEventType {

    /**
     * 연결이 열렸다는 신호.
     *
     * <p>끊긴 동안의 이벤트를 다시 보내지 않기 때문에, 프론트는 이 신호를 받을 때마다 현재 상태를
     * 조회 API 로 다시 읽어 화면을 맞춘다. 최초 연결과 재연결을 구분하지 않는 것은 의도한 것이다.
     * 어느 쪽이든 화면이 할 일이 "다시 읽는다"로 같다.
     */
    CONNECTED,

    // 부스 참여자
    USER_JOINED,
    USER_LEFT,

    // 매칭
    MATCH_SUGGESTED,
    MATCH_ACCEPTED,
    MATCH_REJECTED,

    /**
     * 찔러보기. 자동 매칭과 나누는 이유는 알림 문구와 열어야 하는 화면이 다르기 때문이다.
     *
     * <p>자동 매칭은 "서로의 니즈가 매칭됐어요" 로 {@code /match} 를 열고, 찔러보기는
     * "교환 신청이 왔어요" 로 {@code /poke/received} 를 열어야 한다. 같은 이벤트를 쓰면
     * {@code PushMessage} 가 문구와 경로를 하나로 고정해 둬서 한쪽이 반드시 틀린다.
     */
    POKE_RECEIVED,
    POKE_ACCEPTED,
    POKE_REJECTED,

    // 교환 약속. 만나는 자리(구역)가 정해지거나 바뀌면 EXCHANGE_PLACE_UPDATED 로 알린다.
    EXCHANGE_CREATED,

    /**
     * 시간 조율. <b>넷을 나눈 이유는 알림 문구가 넷 다 달라야 하기 때문이다</b>(시안 204:5026).
     *
     * <p>예전에는 시간과 관련된 모든 변화를 {@code EXCHANGE_TIME_UPDATED} 하나로 보냈다.
     * {@code PushMessage} 가 타입 하나에 문구 하나를 고정해 두기 때문에, 그러면 "조율해 달라"
     * 와 "시간이 맞았다" 와 "시간이 안 맞았다" 가 전부 같은 문구로 나간다.
     *
     * <p>{@code EXCHANGE_TIME_UPDATED} 는 시간이 확정됐을 때만 쓴다.
     */
    EXCHANGE_TIME_REQUESTED,
    EXCHANGE_TIME_MATCHED,
    EXCHANGE_TIME_MISMATCHED,
    EXCHANGE_TIME_UPDATED,

    /**
     * 참가자 한 명이 "이 시간으로 약속!" 을 눌렀다. <b>아직 확정된 것은 아니다.</b>
     *
     * <p>확정은 전원이 눌러야 되고, 그때 나가는 것은 {@code EXCHANGE_TIME_UPDATED} 다. 둘을
     * 나누지 않으면 먼저 누른 사람의 화면이 "약속 시간이 정해졌어요" 를 받고 약속 화면으로
     * 넘어가는데, 서버에는 아직 시각이 없다.
     */
    EXCHANGE_TIME_AGREED,

    /**
     * 참가자 한 명이 고른 칸이 바뀌었다. <b>화면을 맞추기 위한 것이라 알릴 문구가 없다.</b>
     *
     * <p>{@code PushMessage} 에 등록하지 않는다. 시간표는 칸을 누를 때마다 저장되는데, 그 하나
     * 하나를 알림으로 만들면 상대가 다섯 칸을 고르는 동안 내 알림함에 다섯 건이 쌓인다. 알릴
     * 값이 있는 것은 겹치는 시간이 생겼는지 없어졌는지가 바뀐 순간이고, 그건
     * {@code EXCHANGE_TIME_MATCHED} 와 {@code EXCHANGE_TIME_MISMATCHED} 가 맡는다.
     */
    EXCHANGE_SLOTS_UPDATED,

    /** 참가자 한 명이 약속 장소에 도착했다. 시간이 바뀐 것이 아니라 따로 둔다. */
    EXCHANGE_ARRIVED,
    EXCHANGE_PLACE_UPDATED,
    EXCHANGE_COMPLETED,
    EXCHANGE_CANCELLED
}
