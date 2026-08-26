package com.helloiamoneteamnicetomeetyou.hackathon.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 도메인별로 코드 앞자리를 나눠 쓴다. 1000 공통, 2000 사용자, 3000 부스와 구역, 4000 교환,
 * 5000 카드와 보유·희망 등록, 6000 알림이다.
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
    ZONE_IN_USE(HttpStatus.CONFLICT, 3002, "이 구역에서 만나기로 한 약속이 있어 지울 수 없습니다."),

    // Exchange
    EXCHANGE_NOT_FOUND(HttpStatus.NOT_FOUND, 4000, "교환을 찾을 수 없습니다."),
    NOT_EXCHANGE_PARTICIPANT(HttpStatus.FORBIDDEN, 4001, "이 교환의 참가자가 아닙니다."),
    INVALID_TIME_SLOT(HttpStatus.BAD_REQUEST, 4002, "고를 수 없는 시간입니다."),
    NO_OVERLAPPING_TIME(HttpStatus.CONFLICT, 4003, "모두가 되는 시간이 아직 없습니다."),
    EXCHANGE_TIME_ALREADY_CONFIRMED(HttpStatus.CONFLICT, 4004, "이미 시간이 정해진 약속입니다."),
    INVALID_EXCHANGE_PARTICIPANTS(HttpStatus.BAD_REQUEST, 4005, "교환 참가자 구성이 올바르지 않습니다."),
    UNSUPPORTED_MATCHING_SIZE(HttpStatus.BAD_REQUEST, 4006, "2인과 3인 매칭만 지원합니다."),
    EXCHANGE_TIME_NOT_CONFIRMED(HttpStatus.CONFLICT, 4007, "아직 만날 시간이 정해지지 않았습니다."),
    EXCHANGE_ALREADY_FINISHED(HttpStatus.CONFLICT, 4008, "이미 끝난 약속입니다."),
    EXCHANGE_NOT_ACCEPTED(HttpStatus.CONFLICT, 4009, "아직 수락하지 않은 교환입니다."),

    // Poke (찔러보기). 교환 제안이라 교환 대역을 이어 쓴다.
    POKE_NOT_FOUND(HttpStatus.NOT_FOUND, 4010, "찔러보기를 찾을 수 없습니다."),
    POKE_ALREADY_ANSWERED(HttpStatus.CONFLICT, 4011, "이미 응답한 찔러보기입니다."),
    POKE_DUPLICATE_PENDING(HttpStatus.CONFLICT, 4012, "이미 답변을 기다리는 찔러보기가 있습니다."),
    POKE_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, 4013, "자신에게는 찔러볼 수 없습니다."),
    POKE_NOT_RECEIVER(HttpStatus.FORBIDDEN, 4014, "이 찔러보기에 응답할 수 있는 사용자가 아닙니다."),
    POKE_ITEM_NOT_OWNED(HttpStatus.BAD_REQUEST, 4015, "상대가 가지고 있지 않은 카드입니다."),
    POKE_CHOSEN_ITEM_NOT_OFFERED(HttpStatus.BAD_REQUEST, 4016, "상대가 내놓은 카드가 아닙니다."),
    POKE_NO_OFFERABLE_ITEM(HttpStatus.BAD_REQUEST, 4017, "내놓을 카드가 없어 찔러볼 수 없습니다."),
    POKE_ITEM_SOLD_OUT(HttpStatus.CONFLICT, 4018, "상대의 카드가 모두 교환되었습니다."),

    // Item / UserHaveItem / UserWantItem
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, 5000, "카드를 찾을 수 없습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, 5001, "수량은 1개 이상이어야 합니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, 6000, "알림을 찾을 수 없습니다."),
    NOTIFICATION_NOT_RECIPIENT(HttpStatus.FORBIDDEN, 6001, "이 알림의 수신자가 아닙니다.");

    private final HttpStatus httpStatus;
    private final Integer errorCode;
    private final String message;
}
