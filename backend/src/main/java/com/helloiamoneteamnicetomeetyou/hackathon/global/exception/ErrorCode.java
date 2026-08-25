package com.helloiamoneteamnicetomeetyou.hackathon.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 도메인별로 코드 앞자리를 나눠 쓴다. 1000 공통, 2000 사용자, 3000 부스와 구역, 4000 교환이다.
 */
@Getter
@RequiredArgsConstructor
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
    INVALID_TIME_SLOT(HttpStatus.BAD_REQUEST, 4002, "고를 수 없는 시간입니다."),
    NO_OVERLAPPING_TIME(HttpStatus.CONFLICT, 4003, "모두가 되는 시간이 아직 없습니다."),
    EXCHANGE_TIME_ALREADY_CONFIRMED(HttpStatus.CONFLICT, 4004, "이미 시간이 정해진 약속입니다."),
    INVALID_EXCHANGE_PARTICIPANTS(HttpStatus.BAD_REQUEST, 4005, "교환 참가자 구성이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final Integer errorCode;
    private final String message;
}
