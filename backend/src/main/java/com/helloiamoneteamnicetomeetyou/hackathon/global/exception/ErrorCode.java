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
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 1004, "요청한 리소스를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final Integer errorCode;
    private final String message;
}
