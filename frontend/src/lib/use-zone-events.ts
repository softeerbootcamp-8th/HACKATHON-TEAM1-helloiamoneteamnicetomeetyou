import { useEffect, useRef, useState } from 'react'

import { openZoneEventStream, SSE_EVENT_TYPES, type SseEventHandlers, type SseStatus } from './sse'

/**
 * 대기장소 실시간 알림을 구독한다.
 *
 * ```tsx
 * const status = useZoneEvents(zoneId, userId, {
 *   CONNECTED: () => refetchZone(),
 *   USER_JOINED: () => refetchZone(),
 *   MATCH_SUGGESTED: (data) => setSuggestion(data as MatchSuggestion),
 * })
 * ```
 *
 * **`CONNECTED` 는 거의 항상 처리해야 한다.** 서버가 끊긴 동안의 이벤트를 다시 보내지 않기
 * 때문에, 이 신호를 받았을 때 현재 상태를 다시 읽지 않으면 재연결 이후 화면이 옛 상태로 남는다.
 *
 * `zoneId` 나 `userId` 가 아직 없으면(로딩 중이면) `null` 을 넘긴다. 연결하지 않고 기다린다.
 */
export function useZoneEvents(
  zoneId: number | null,
  userId: string | null,
  handlers: SseEventHandlers,
): SseStatus {
  const [status, setStatus] = useState<SseStatus>('connecting')

  // 화면이 다시 그려질 때마다 handlers 객체가 새로 만들어지는데, 그걸 effect 의 의존성에 넣으면
  // 매 렌더마다 연결을 끊고 다시 붙게 된다. ref 에 최신 것만 담아 두고 effect 는 id 로만 돈다.
  const handlersRef = useRef(handlers)

  // 렌더 중에 ref 를 건드리면 안 되기 때문에(react-hooks/refs) 의존성 없는 effect 에서 갱신한다.
  // 렌더가 끝날 때마다 돈다.
  useEffect(() => {
    handlersRef.current = handlers
  })

  useEffect(() => {
    if (zoneId === null || userId === null) return

    const close = openZoneEventStream(
      zoneId,
      userId,
      // 이벤트 이름 전부에 리스너를 걸어 두고, 실제 처리는 그때그때 ref 에 든 것으로 넘긴다.
      // 연결할 때의 handlers 키만 걸면, 조건부로 나중에 추가된 핸들러가 영영 안 불린다.
      Object.fromEntries(
        SSE_EVENT_TYPES.map((type) => [type, (data: unknown) => handlersRef.current[type]?.(data)]),
      ) as SseEventHandlers,
      setStatus,
    )

    return close
  }, [zoneId, userId])

  return status
}
