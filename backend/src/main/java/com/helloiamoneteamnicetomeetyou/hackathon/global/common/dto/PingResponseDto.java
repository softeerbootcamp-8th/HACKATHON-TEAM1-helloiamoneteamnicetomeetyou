package com.helloiamoneteamnicetomeetyou.hackathon.global.common.dto;

import java.time.LocalDateTime;

/**
 * 프론트가 백엔드까지 실제로 닿았는지 확인할 때 받는 응답이다.
 *
 * @param message    고정 문자열 "pong"
 * @param serverTime 서버가 응답을 만든 시각. 화면에 찍히면 캐시가 아니라 방금 온 응답이라는 뜻이다
 */
public record PingResponseDto(String message, LocalDateTime serverTime) {

    public static PingResponseDto of(LocalDateTime serverTime) {
        return new PingResponseDto("pong", serverTime);
    }
}
