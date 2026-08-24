package com.helloiamoneteamnicetomeetyou.hackathon.global.exception;

import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonResponse<Void>> handleApplicationException(ApplicationException e) {
        log.warn("ApplicationException: {}", e.getMessage());
        ErrorType errorType = e.getErrorType();
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(CommonResponse.error(errorType));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("ValidationException: {}", e.getMessage());
        List<ValidationError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ValidationError.of(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INVALID_INPUT, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_TYPE.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INVALID_TYPE));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("MethodNotSupportedException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        log.error("UnhandledException: {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
