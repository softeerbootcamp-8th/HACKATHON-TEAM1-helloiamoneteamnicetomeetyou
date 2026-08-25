package com.helloiamoneteamnicetomeetyou.hackathon.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2000, "등록되지 않은 사용자입니다."),

    // Item
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, 4000, "카드를 찾을 수 없습니다."),

    // UserHaveItem / UserWantItem
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, 5000, "수량은 1개 이상이어야 합니다.");

    private final HttpStatus httpStatus;
    private final Integer errorCode;
    private final String message;
}
