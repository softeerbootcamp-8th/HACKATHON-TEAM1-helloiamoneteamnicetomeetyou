/**
 * 대기장소 실시간 알림(SSE) 의 타입과 주소를 모아 둔다.
 *
 * 화면에 붙이는 것은 `useZoneEvents` 훅이고, 여기에는 백엔드와 맞춰야 하는 것만 둔다.
 */
import { BASE_URL } from './api'

/**
 * 백엔드 `SseEventType` enum 과 같은 목록이다. 한쪽만 고치면 조용히 어긋나므로 같이 고친다.
 */
export const SSE_EVENT_TYPES = [
  // 연결이 열렸다는 신호. 최초 연결과 재연결을 구분하지 않는다.
  'CONNECTED',

  // 대기장소 참여자
  'USER_JOINED',
  'USER_LEFT',

  // 매칭
  'MATCH_SUGGESTED',
  'MATCH_ACCEPTED',
  'MATCH_REJECTED',

  // 교환 약속
  'EXCHANGE_CREATED',
  'EXCHANGE_TIME_UPDATED',
  'EXCHANGE_PLACE_UPDATED',
  'EXCHANGE_COMPLETED',
  'EXCHANGE_CANCELLED',
] as const

export type SseEventType = (typeof SSE_EVENT_TYPES)[number]

/**
 * 받고 싶은 이벤트만 골라 담는다.
 *
 * 데이터 타입을 `unknown` 으로 두는 것은, 어떤 모양이 올지는 그 이벤트를 보내는 도메인이 정하기
 * 때문이다. 각 기능에서 자기 이벤트의 타입을 정의해 좁혀 쓰면 된다.
 */
export type SseEventHandlers = Partial<Record<SseEventType, (data: unknown) => void>>

export type SseStatus = 'connecting' | 'open' | 'error'

export function zoneEventStreamUrl(zoneId: number, userId: string): string {
  return `${BASE_URL}/api/zones/${zoneId}/subscribe?userId=${encodeURIComponent(userId)}`
}

/**
 * 서버가 보낸 문자열을 JSON 으로 푼다.
 *
 * 실패하면 원본 문자열을 그대로 돌려준다. 알림 하나의 모양이 예상과 달라서 화면 전체가 죽는
 * 것보다는, 그 알림만 무시되는 편이 낫다.
 */
function parseEventData(raw: string): unknown {
  try {
    return JSON.parse(raw)
  } catch {
    return raw
  }
}

/**
 * `EventSource` 를 열고 이벤트 이름별 리스너를 건다. 반환하는 함수를 부르면 연결을 닫는다.
 *
 * React 밖에서도 쓸 수 있게 훅과 분리해 뒀다. 화면에서는 `useZoneEvents` 를 쓰면 된다.
 */
export function openZoneEventStream(
  zoneId: number,
  userId: string,
  handlers: SseEventHandlers,
  onStatusChange?: (status: SseStatus) => void,
): () => void {
  const source = new EventSource(zoneEventStreamUrl(zoneId, userId))

  onStatusChange?.('connecting')
  source.onopen = () => onStatusChange?.('open')

  // EventSource 는 연결이 끊기면 스스로 다시 붙는다. 그래서 error 가 곧 실패는 아니고,
  // readyState 가 CLOSED 일 때만 재시도를 포기한 것이다(주소가 틀렸거나 서버가 4xx 를 준 경우).
  source.onerror = () => {
    onStatusChange?.(source.readyState === EventSource.CLOSED ? 'error' : 'connecting')
  }

  for (const type of SSE_EVENT_TYPES) {
    const handler = handlers[type]
    if (!handler) continue

    source.addEventListener(type, (event) => {
      handler(parseEventData((event as MessageEvent<string>).data))
    })
  }

  return () => source.close()
}
