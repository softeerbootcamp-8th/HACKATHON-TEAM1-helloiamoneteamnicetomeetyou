import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'

import { useCatalog } from '@/features/catalog/useCatalog'
import { messageOf } from '@/lib/api'
import { useBoothEvents } from '@/lib/use-booth-events'

import { fetchNotifications, markNotificationRead, type ServerNotification } from './api'
import { NotificationContext } from './NotificationContext'

/**
 * 서버에 쌓인 알림을 들고 있고, 실시간 알림을 받으면 다시 읽는다.
 *
 * 기기가 꺼져 있던 동안 온 것은 SSE 로 다시 오지 않는다. 그래서 화면이 다시 붙었을 때
 * (`CONNECTED`) 목록을 다시 읽어야 놓친 알림까지 보인다. `PokeProvider` 와 같은 이유로,
 * 나머지 이벤트도 알림 내용으로 화면을 고치지 않고 신호로만 쓴다.
 */
export function NotificationProvider({ children }: { children: ReactNode }) {
  const { state: catalog, userId } = useCatalog()

  const [notifications, setNotifications] = useState<ServerNotification[]>([])
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  const boothId = catalog.status === 'ready' ? catalog.boothId : null
  const ready = boothId !== null

  const refresh = useCallback(() => setAttempt((n) => n + 1), [])
  const clearError = useCallback(() => setError(null), [])

  useEffect(() => {
    if (boothId === null) return

    const controller = new AbortController()
    const { signal } = controller

    void (async () => {
      try {
        const page = await fetchNotifications(userId, signal)
        if (signal.aborted) return
        // 읽은 것은 목록에서 뺀다. 서버는 읽음 표시만 하고 계속 내려주기 때문에, 걸러 두지
        // 않으면 스와이프로 치운 알림이 다음 새로고침에 다시 나타난다.
        setNotifications(page.content.filter((n) => !n.read))
      } catch (err) {
        if (signal.aborted) return
        setError(messageOf(err))
      }
    })()

    // StrictMode 가 개발 모드에서 effect 를 두 번 돌린다. 먼저 뜬 요청은 여기서 끊는다.
    return () => controller.abort()
  }, [boothId, userId, attempt])

  // 알림이 오면 다시 읽는다. 어떤 종류든 하는 일이 같아서 핸들러를 하나로 둔다.
  const onBoothEvent = useCallback(() => refresh(), [refresh])

  useBoothEvents(boothId, ready ? userId : null, {
    CONNECTED: onBoothEvent,
    MATCH_SUGGESTED: onBoothEvent,
    MATCH_ACCEPTED: onBoothEvent,
    MATCH_REJECTED: onBoothEvent,
    POKE_RECEIVED: onBoothEvent,
    POKE_ACCEPTED: onBoothEvent,
    POKE_REJECTED: onBoothEvent,
    EXCHANGE_CREATED: onBoothEvent,
    EXCHANGE_TIME_REQUESTED: onBoothEvent,
    EXCHANGE_TIME_MATCHED: onBoothEvent,
    EXCHANGE_TIME_MISMATCHED: onBoothEvent,
    EXCHANGE_TIME_UPDATED: onBoothEvent,
    EXCHANGE_PLACE_UPDATED: onBoothEvent,
    EXCHANGE_CANCELLED: onBoothEvent,
  })

  // 스와이프로 지운 알림은 서버에 읽음 처리하고 화면에서도 뺀다.
  const markRead = useCallback(
    async (id: number) => {
      try {
        await markNotificationRead(id, userId)
        setNotifications((prev) => prev.filter((n) => n.id !== id))
      } catch (err) {
        setError(messageOf(err))
      }
    },
    [userId],
  )

  // 목록에 읽지 않은 것만 남으므로 길이가 곧 개수다.
  const unreadCount = notifications.length

  const value = useMemo(
    () => ({ notifications, unreadCount, ready, refresh, markRead, error, clearError }),
    [notifications, unreadCount, ready, refresh, markRead, error, clearError],
  )

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}
