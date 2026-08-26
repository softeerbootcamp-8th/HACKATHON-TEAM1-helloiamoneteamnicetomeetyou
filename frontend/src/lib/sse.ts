/**
 * 부스 실시간 알림(SSE) 의 타입과 주소를 모아 둔다.
 *
 * 화면에 붙이는 것은 `useBoothEvents` 훅이고, 여기에는 백엔드와 맞춰야 하는 것만 둔다.
 */
import { BASE_URL } from './api'

/**
 * 백엔드 `SseEventType` enum 과 같은 목록이다. 한쪽만 고치면 조용히 어긋나므로 같이 고친다.
 */
export const SSE_EVENT_TYPES = [
  // 연결이 열렸다는 신호. 최초 연결과 재연결을 구분하지 않는다.
  'CONNECTED',

  // 부스 참여자
  'USER_JOINED',
  'USER_LEFT',

  // 매칭
  'MATCH_SUGGESTED',
  'MATCH_ACCEPTED',
  'MATCH_REJECTED',

  // 찔러보기. 자동 매칭과 문구도 열어야 하는 화면도 달라서 따로 둔다.
  'POKE_RECEIVED',
  'POKE_ACCEPTED',
  'POKE_REJECTED',

  // 교환 약속. 만나는 자리(구역)가 정해지거나 바뀌면 EXCHANGE_PLACE_UPDATED 로 온다.
  'EXCHANGE_CREATED',

  // 시간 조율. 알림 문구가 넷 다 달라서 나눠 둔다. EXCHANGE_TIME_UPDATED 는 시간 확정 전용이다.
  'EXCHANGE_TIME_REQUESTED',
  'EXCHANGE_TIME_MATCHED',
  'EXCHANGE_TIME_MISMATCHED',
  'EXCHANGE_TIME_UPDATED',

  // 참가자 한 명이 약속 장소에 도착했다.
  'EXCHANGE_ARRIVED',
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

/**
 * 구독 단위는 부스다. 지도에서 고르는 구역(zone)이 아니다.
 *
 * 구역은 만나는 자리라 사용자가 옮겨 다니는데, 그때마다 연결을 끊었다 다시 맺으면 그 사이
 * 이벤트를 놓친다. 어떤 구역 이야기인지는 이벤트 데이터에 담겨 온다.
 */
export function boothEventStreamUrl(boothId: number, userId: string): string {
  return `${BASE_URL}/api/booths/${boothId}/subscribe?userId=${encodeURIComponent(userId)}`
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
/** 한 구독자. `openBoothEventStream` 호출 하나에 대응한다. */
type Subscriber = {
  handlers: SseEventHandlers
  onStatusChange?: (status: SseStatus) => void
}

/** 부스와 사용자가 같은 구독자들이 함께 쓰는 연결 하나. */
type SharedStream = {
  source: EventSource
  subscribers: Set<Subscriber>
  status: SseStatus
}

/**
 * 열려 있는 연결. 키는 `${boothId}:${userId}` 다.
 *
 * 연결을 여기 모아 두는 이유는 구독 지점이 여럿이기 때문이다. 지금은 네 곳
 * (`PokeProvider`, `NotificationProvider`, `StoreProvider`, `AppShell`)이 각자
 * 자기 이벤트만 구독하는데, 그 수만큼 연결이 열리면 브라우저의 오리진당 동시 연결
 * 한도(6)를 금방 넘긴다. 남은 자리가 없으면 SSE 가 아니라 **일반 API 요청이 밀린다.**
 */
const streams = new Map<string, SharedStream>()

function streamKey(boothId: number, userId: string): string {
  return `${boothId}:${userId}`
}

/**
 * 연결을 새로 만들고 이벤트 이름 전부에 리스너를 건다.
 *
 * 구독자가 그 이벤트를 원하는지는 받은 뒤에 각자 판단한다. 붙을 때의 핸들러 키만 리스너로
 * 걸면, 나중에 붙은 구독자가 원하는 이벤트는 리스너가 없어서 영영 오지 않는다.
 */
function createStream(key: string, boothId: number, userId: string): SharedStream {
  const source = new EventSource(boothEventStreamUrl(boothId, userId))
  const stream: SharedStream = { source, subscribers: new Set(), status: 'connecting' }

  const publishStatus = (status: SseStatus) => {
    stream.status = status
    for (const subscriber of stream.subscribers) {
      subscriber.onStatusChange?.(status)
    }
  }

  source.onopen = () => publishStatus('open')

  // EventSource 는 연결이 끊기면 스스로 다시 붙는다. 그래서 error 가 곧 실패는 아니고,
  // readyState 가 CLOSED 일 때만 재시도를 포기한 것이다(주소가 틀렸거나 서버가 4xx 를 준 경우).
  source.onerror = () => {
    publishStatus(source.readyState === EventSource.CLOSED ? 'error' : 'connecting')
  }

  for (const type of SSE_EVENT_TYPES) {
    source.addEventListener(type, (event) => {
      const data = parseEventData((event as MessageEvent<string>).data)

      // 순회 중에 구독자가 빠질 수 있어서(핸들러가 정리 함수를 부르는 경우) 사본을 돈다.
      for (const subscriber of [...stream.subscribers]) {
        subscriber.handlers[type]?.(data)
      }
    })
  }

  streams.set(key, stream)

  return stream
}

/**
 * 부스 이벤트를 구독한다. 반환하는 함수를 부르면 구독을 끊는다.
 *
 * **같은 `boothId` 와 `userId` 로 여러 번 불러도 연결은 하나다.** 먼저 부른 쪽이 연 연결에
 * 얹히고, 마지막 구독자가 떠날 때 닫힌다.
 *
 * React 밖에서도 쓸 수 있게 훅과 분리해 뒀다. 화면에서는 `useBoothEvents` 를 쓰면 된다.
 */
export function openBoothEventStream(
  boothId: number,
  userId: string,
  handlers: SseEventHandlers,
  onStatusChange?: (status: SseStatus) => void,
): () => void {
  const key = streamKey(boothId, userId)
  const stream = streams.get(key) ?? createStream(key, boothId, userId)

  const subscriber: Subscriber = { handlers, onStatusChange }
  stream.subscribers.add(subscriber)

  // 이미 열려 있는 연결에 얹히면 onopen 을 다시 받지 못한다. 지금 상태를 여기서 한 번
  // 알려주지 않으면 늦게 붙은 구독자만 영영 'connecting' 으로 남는다.
  onStatusChange?.(stream.status)

  return () => {
    stream.subscribers.delete(subscriber)

    if (stream.subscribers.size > 0) return

    // 아무도 안 보는 연결은 닫는다. 열어 둔 채로 두면 서버가 타임아웃(30분)까지 들고 있다.
    stream.source.close()

    // 같은 키로 이미 새 연결이 만들어졌다면 그것을 지우지 않는다.
    if (streams.get(key) === stream) {
      streams.delete(key)
    }
  }
}
