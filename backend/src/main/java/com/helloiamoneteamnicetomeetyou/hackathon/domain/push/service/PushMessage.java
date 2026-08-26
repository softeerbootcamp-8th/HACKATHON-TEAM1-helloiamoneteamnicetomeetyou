package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service;

import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 실시간 이벤트를 잠금 화면에 띄울 문구로 옮긴다.
 *
 * <p><b>여기 없는 이벤트는 푸시하지 않는다.</b> {@code CONNECTED} 같은 연결 신호나
 * {@code USER_JOINED} 처럼 부스 전체에 뿌리는 것은 알릴 내용이 아니고,
 * {@code EXCHANGE_COMPLETED} 는 본인이 현장에서 방금 한 행동이라 알림이 오면 어색하다.
 *
 * <p>문구를 {@link SseEventType} 에 넣지 않은 것은, 그 enum 이 프론트와 이름을 맞추는 용도라
 * 알림 문구까지 섞으면 "새 이벤트가 필요하면 양쪽을 같이 고친다" 는 규칙이 흐려지기 때문이다.
 */
@Getter
@RequiredArgsConstructor
public enum PushMessage {

    MATCH_SUGGESTED(SseEventType.MATCH_SUGGESTED, "서로의 니즈가 매칭됐어요!", "탭하여 확인해 보세요", "/match"),
    MATCH_ACCEPTED(SseEventType.MATCH_ACCEPTED, "교환이 성사됐어요", "만날 자리를 정해 주세요", "/match"),
    MATCH_REJECTED(SseEventType.MATCH_REJECTED, "상대가 교환을 거절했어요", "다시 상대를 찾아볼까요", "/home"),
    // 찔러보기. 문구는 시안(204:5032, 204:5154)을 그대로 옮겼다.
    POKE_RECEIVED(SseEventType.POKE_RECEIVED, "교환 신청이 왔어요~", "탭하여 확인해 보세요", "/poke/received"),
    POKE_ACCEPTED(SseEventType.POKE_ACCEPTED, "상대방이 내 신청을 받아들였어요!", "만날 자리를 정해 주세요", "/match"),
    POKE_REJECTED(SseEventType.POKE_REJECTED, "상대가 교환을 거절했어요", "다른 상대를 찾아보세요", "/home"),

    EXCHANGE_CREATED(SseEventType.EXCHANGE_CREATED, "교환 약속이 잡혔어요", "장소와 시간을 확인해 주세요", "/appointment"),
    EXCHANGE_TIME_UPDATED(SseEventType.EXCHANGE_TIME_UPDATED, "약속 시간이 바뀌었어요", "새 시간을 확인해 주세요", "/appointment"),
    EXCHANGE_PLACE_UPDATED(SseEventType.EXCHANGE_PLACE_UPDATED, "만나는 자리가 바뀌었어요", "어디서 만나는지 다시 확인해 주세요", "/appointment"),
    EXCHANGE_CANCELLED(SseEventType.EXCHANGE_CANCELLED, "교환이 취소됐어요", "다시 상대를 찾아보세요", "/home");

    private final SseEventType eventType;
    private final String title;
    private final String body;

    /** 알림을 눌렀을 때 열 화면. 프론트 ROUTE_ORDER 에 있는 경로여야 한다. */
    private final String url;

    public static Optional<PushMessage> from(SseEventType eventType) {
        return Arrays.stream(values())
                .filter(message -> message.eventType == eventType)
                .findFirst();
    }
}
