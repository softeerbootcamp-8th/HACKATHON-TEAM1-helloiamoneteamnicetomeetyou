package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

/**
 * 어느 교환에 대한 이벤트인지 밝히는 payload.
 *
 * <p>알림함이 이걸 본다. 저장된 알림에 교환 번호를 같이 남겨 두면, 그 교환이 끝났을 때
 * ({@code EXCHANGE_COMPLETED}, {@code EXCHANGE_CANCELLED}, {@code MATCH_REJECTED}) 딸린 알림을
 * 한 번에 정리할 수 있다. 없으면 무엇을 지워야 할지 알 수 없어서, 이미 끝난 약속의 중간 단계
 * 알림이 대기 화면에 계속 남는다.
 *
 * <p><b>교환에 매인 이벤트의 payload 는 전부 이걸 구현한다.</b> 어떤 것은 {@code Map} 으로
 * 어떤 것은 DTO 로 보내면, 알림함이 payload 모양마다 꺼내는 방법을 따로 알아야 한다.
 */
public interface ExchangeScoped {

    /** 교환 번호. 아직 교환이 없는 단계의 이벤트(찔러보기 수신 등)면 {@code null} 이다. */
    Long exchangeId();
}
