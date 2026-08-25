package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto;

/**
 * 부스의 카드 목록이 바뀌었다는 알림에 실어 보내는 내용이다.
 *
 * <p><b>일부러 작게 둔다.</b> 화면은 이 값으로 목록을 고치지 않고, 신호를 받으면 조회 API 로
 * 현재 상태를 다시 읽는다. 서버가 끊긴 동안의 이벤트를 재전송하지 않기 때문에 이벤트를 하나씩
 * 반영하는 방식으로는 연결이 한 번 끊기면 화면이 옛 상태로 남는다.
 *
 * <p>각 줄의 "내 희망 카드인가" 와 "내가 줄 수 있는 카드" 는 보는 사람마다 다르게 계산되는
 * 값이라, 애초에 한 이벤트에 담아 모두에게 뿌릴 수가 없다.
 *
 * @param boothId 목록이 바뀐 부스
 * @param itemId  방금 등록된 카드
 */
public record BoothRosterChangedDto(Long boothId, Long itemId) {}
