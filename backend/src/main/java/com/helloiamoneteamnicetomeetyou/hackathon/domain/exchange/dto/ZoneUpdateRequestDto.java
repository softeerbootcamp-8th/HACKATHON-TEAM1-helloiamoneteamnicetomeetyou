package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto;

import java.util.UUID;

/**
 * 만날 자리를 바꾼다.
 *
 * <p>{@code userId} 는 누가 바꾸는지다. 참가자만 바꿀 수 있어서 서버가 이걸로 확인한다.
 * 어느 구역인지는 {@code zoneId} 로 보내고, 이름이나 좌표는 보내지 않는다. 그건 어드민이
 * 고치는 값이라 화면이 들고 있던 옛 값을 되돌려 쓰면 안 된다.
 */
public record ZoneUpdateRequestDto(UUID userId, Long zoneId) {
}
