import { useEffect, useRef, useState } from 'react'

import { openBoothEventStream, SSE_EVENT_TYPES, type SseEventHandlers, type SseStatus } from './sse'

/**
 * 부스 실시간 알림을 구독한다.
 *
 * ```tsx
 * const status = useBoothEvents(boothId, userId, {
 *   CONNECTED: () => refetchBooth(),
 *   USER_JOINED: () => refetchBooth(),
 *   MATCH_SUGGESTED: (data) => setSuggestion(data as MatchSuggestion),
 * })
 * ```
 *
 * **`CONNECTED` 는 거의 항상 처리해야 한다.** 서버가 끊긴 동안의 이벤트를 다시 보내지 않기
 * 때문에, 이 신호를 받았을 때 현재 상태를 다시 읽지 않으면 재연결 이후 화면이 옛 상태로 남는다.
 *
 * 구독 단위는 부스라 사용자가 지도에서 구역을 옮겨도 이 훅을 다시 부를 필요가 없다.
 *
 * `boothId` 나 `userId` 가 아직 없으면(로딩 중이면) `null` 을 넘긴다. 연결하지 않고 기다린다.
 */
export function useBoothEvents(
  boothId: number | null,
  userId: string | null,
  handlers: SseEventHandlers,
): SseStatus {
  const [status, setStatus] = useState<SseStatus>('connecting')

  const handlersRef = useRef(handlers)

  // 렌더 중에 ref 를 건드리면 안 되기 때문에(react-hooks/refs) 의존성 없는 effect 에서 갱신한다.
  // 렌더가 끝날 때마다 돈다.
  useEffect(() => {
    handlersRef.current = handlers
  })

  useEffect(() => {
    if (boothId === null || userId === null) return

    const close = openBoothEventStream(
      boothId,
      userId,
      // 이벤트 이름 전부에 리스너를 걸어 두고, 실제 처리는 그때그때 ref 에 든 것으로 넘긴다.
      // 연결할 때의 handlers 키만 걸면, 조건부로 나중에 추가된 핸들러가 영영 안 불린다.
      Object.fromEntries(
        SSE_EVENT_TYPES.map((type) => [type, (data: unknown) => handlersRef.current[type]?.(data)]),
      ) as SseEventHandlers,
      setStatus,
    )

    return close
  }, [boothId, userId])

  return status
}
