package com.helloiamoneteamnicetomeetyou.hackathon.global.exception;

import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonResponse<Void>> handleApplicationException(
            ApplicationException e, HttpServletRequest request) {

        ErrorType errorType = e.getErrorType();

        if (errorType.getHttpStatus().is5xxServerError()) {
            log.error("애플리케이션 예외 발생 - [{}] {} {} ({})",
                    request.getMethod(), request.getRequestURI(),
                    errorType.getErrorCode(), errorType.getMessage(), e);
        } else {
            log.warn("애플리케이션 예외 발생 - [{}] {} {} ({})",
                    request.getMethod(), request.getRequestURI(),
                    errorType.getErrorCode(), errorType.getMessage());
        }

        return ResponseEntity.status(errorType.getHttpStatus())
                .body(CommonResponse.error(errorType));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        List<ValidationError> errors = e.getBindingResult().getAllErrors()
                .stream()
                .map(GlobalExceptionHandler::toValidationError)
                .toList();

        log.warn("유효성 검증 실패 - [{}] {} errors={}",
                request.getMethod(), request.getRequestURI(), errors);

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INVALID_INPUT, errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<CommonResponse<Void>> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpServletRequest request) {

        List<ValidationError> errors = e.getParameterValidationResults()
                .stream()
                .flatMap(GlobalExceptionHandler::toValidationErrors)
                .toList();

        log.warn("유효성 검증 실패 - [{}] {} errors={}",
                request.getMethod(), request.getRequestURI(), errors);

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INVALID_INPUT, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        log.warn("요청 바디 파싱 실패 - [{}] {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {

        log.warn("허용되지 않은 HTTP 메서드 - [{}] {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        log.warn("경로 변수 타입 불일치 - [{}] {} name={} value={}",
                request.getMethod(), request.getRequestURI(), e.getName(), e.getValue());

        return ResponseEntity.status(ErrorCode.INVALID_TYPE.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INVALID_TYPE));
    }

    /**
     * 필수 쿼리 파라미터가 빠진 경우다. 잡아 주지 않으면 아래 {@code Exception} 핸들러로 떨어져서,
     * 요청을 잘못 보낸 것인데 500 과 "서버 내부 오류" 가 나간다. 프론트가 원인을 알 수 없다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {

        log.warn("필수 요청 파라미터 누락 - [{}] {} name={}",
                request.getMethod(), request.getRequestURI(), e.getParameterName());

        List<ValidationError> errors =
                List.of(new ValidationError(e.getParameterName(), "필수 값입니다."));

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INVALID_INPUT, errors));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException e, HttpServletRequest request) {

        log.warn("리소스를 찾을 수 없음 - [{}] {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /**
     * SSE(구독) 처럼 끝나지 않는 요청에서 클라이언트가 이미 나간 뒤 서버가 쓰기를 시도하면 난다.
     *
     * <p>이 시점엔 연결이 끊긴 뒤라 응답 바디를 쓸 곳이 없다. 몸체를 채워 돌려주면(=아래
     * {@code handleException} 처럼) {@code text/event-stream} 으로 이미 커밋된 응답에 JSON 을
     * 쓰려다 {@code HttpMessageNotWritableException} 이 한 번 더 나면서 정상적인 연결 종료가
     * 오류처럼 로그에 두 번 찍힌다. 여기서는 반환값 없이 조용히 흘려보낸다.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException e, HttpServletRequest request) {

        log.debug("비동기 연결이 이미 끊어짐 - [{}] {}", request.getMethod(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(
            Exception e, HttpServletRequest request) {

        log.error("예상치 못한 예외 발생 - [{}] {} ({})",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private static Stream<ValidationError> toValidationErrors(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            return parameterErrors.getAllErrors().stream()
                    .map(GlobalExceptionHandler::toValidationError);
        }

        String field = Objects.requireNonNullElse(
                result.getMethodParameter().getParameterName(), "unknown");

        return result.getResolvableErrors().stream()
                .map(error -> new ValidationError(field, error.getDefaultMessage()));
    }

    private static ValidationError toValidationError(ObjectError error) {
        String field = error instanceof FieldError fieldError
                ? fieldError.getField()
                : error.getObjectName();

        return new ValidationError(field, error.getDefaultMessage());
    }
}
