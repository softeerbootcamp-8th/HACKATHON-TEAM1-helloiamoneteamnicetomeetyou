import { apiData, apiVoid } from '@/lib/api'
import type { SseEventType } from '@/lib/sse'

/** 서버가 내려주는 목록 껍데기. 형식은 `contracts.md` 가 기준이다. */
export type PageResponse<T> = {
  content: T[]
  /** 커서 방식으로 바꿀 때를 위해 자리만 잡아 둔 값이라 지금은 항상 null 이다. */
  nextCursor: string | null
  hasNext: boolean
  /** 요청한 크기가 아니라 실제로 담긴 개수다. */
  size: number
}

/**
 * 저장된 알림 한 건. `type` 은 이 알림을 만든 실시간 이벤트 이름과 같다.
 *
 * `title`, `body` 는 알림을 보낼 때의 문구를 그대로 굳혀 둔 값이라, 나중에 문구가
 * 바뀌어도 이미 온 알림은 받은 시점 그대로 보인다.
 */
export type ServerNotification = {
  id: number
  type: SseEventType
  title: string
  body: string
  read: boolean
  createdAt: string
}

export function fetchNotifications(
  userId: string,
  signal?: AbortSignal,
): Promise<PageResponse<ServerNotification>> {
  const query = new URLSearchParams({ userId, page: '0', size: '20' })
  return apiData<PageResponse<ServerNotification>>(`/api/notifications?${query}`, { signal })
}

export function markNotificationRead(
  notificationId: number,
  userId: string,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid(`/api/notifications/${notificationId}`, {
    method: 'PATCH',
    body: JSON.stringify({ userId }),
    signal,
  })
}
