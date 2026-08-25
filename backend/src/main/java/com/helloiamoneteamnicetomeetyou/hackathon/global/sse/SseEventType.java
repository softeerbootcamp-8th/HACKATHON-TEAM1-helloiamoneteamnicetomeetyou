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
    EXCHANGE_TIME_UPDATED,
    EXCHANGE_PLACE_UPDATED,
    EXCHANGE_COMPLETED,
    EXCHANGE_CANCELLED
}
