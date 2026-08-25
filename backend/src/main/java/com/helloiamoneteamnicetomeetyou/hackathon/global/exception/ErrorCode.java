package com.helloiamoneteamnicetomeetyou.hackathon.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
/**
 * 도메인별로 코드 앞자리를 나눠 쓴다. 1000 공통, 2000 사용자, 3000 부스와 구역, 4000 교환,
 * 5000 카드와 보유·희망 등록이다.
 */
public enum ErrorCode implements ErrorType {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, 1000, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1001, "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 1002, "허용되지 않는 HTTP 메서드입니다."),
    INVALID_TYPE(HttpStatus.BAD_REQUEST, 1003, "잘못된 타입입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 1004, "요청한 리소스를 찾을 수 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2000, "사용자를 찾을 수 없습니다."),

    // Booth / Zone
    BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, 3000, "부스를 찾을 수 없습니다."),
    ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, 3001, "교환 장소를 찾을 수 없습니다."),

    // Exchange
    EXCHANGE_NOT_FOUND(HttpStatus.NOT_FOUND, 4000, "교환을 찾을 수 없습니다."),
    NOT_EXCHANGE_PARTICIPANT(HttpStatus.FORBIDDEN, 4001, "이 교환의 참가자가 아닙니다."),
    UNSUPPORTED_MATCHING_SIZE(HttpStatus.BAD_REQUEST, 4006, "2인과 3인 매칭만 지원합니다."),

    // Item / UserHaveItem / UserWantItem
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, 5000, "카드를 찾을 수 없습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, 5001, "수량은 1개 이상이어야 합니다.");

    private final HttpStatus httpStatus;
    private final Integer errorCode;
    private final String message;
}
