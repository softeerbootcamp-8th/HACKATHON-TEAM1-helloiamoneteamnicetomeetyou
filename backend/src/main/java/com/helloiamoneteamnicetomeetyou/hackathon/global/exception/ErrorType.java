package com.helloiamoneteamnicetomeetyou.hackathon.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorType {

    HttpStatus getHttpStatus();
    Integer getErrorCode();
    String getMessage();
}
