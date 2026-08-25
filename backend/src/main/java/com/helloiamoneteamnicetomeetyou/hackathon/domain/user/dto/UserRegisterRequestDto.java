package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.dto;

import java.util.UUID;

/**
 * 앞으로 만드는 모든 API 가 이렇게 {@code userId} 를 첫 필드로 받는다.
 *
 * <p>다만 GET 은 body 를 실을 수 없어서 쿼리 파라미터로 받는다.
 *
 * @param userId 클라이언트가 만들어 localStorage 에 들고 다니는 값
 */
public record UserRegisterRequestDto(UUID userId) {}
