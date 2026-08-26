package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.dto;

import java.util.UUID;

/**
 * 사용자 등록 요청이다.
 *
 * <p>{@code username} 은 선택이다. 약속 화면에서 상대 줄의 라벨로 쓰려고 나중에 더한 것이라,
 * 안 보내던 클라이언트는 그대로 두어도 된다. 안 보내면 서버가 기존 이름을 지우지 않는다.
 */
public record UserRegisterRequestDto(UUID userId, String username) {
}
