package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.dto;

import java.util.UUID;

/**
 * 브라우저의 {@code PushSubscription.toJSON()} 에서 꺼낸 값을 그대로 보낸다.
 *
 * <p>{@code p256dh} 와 {@code auth} 는 스펙상 이미 base64url(패딩 없음)이다. 프론트에서
 * {@code getKey()} 로 ArrayBuffer 를 받아 직접 인코딩하면 표준 base64 가 나와서 서버가 못 읽는다.
 */
public record PushSubscribeRequestDto(UUID userId, String endpoint, String p256dh, String auth) {}
