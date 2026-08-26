import { createContext } from 'react'

import type { ServerNotification } from './api'

export type NotificationValue = {
  notifications: ServerNotification[]
  /** 읽지 않은 알림 개수. 종 아이콘 뱃지가 쓴다 */
  unreadCount: number
  /** 서버 연동이 준비됐는지 */
  ready: boolean
  /** 목록을 다시 읽는다. 실시간 알림을 받았을 때 부른다 */
  refresh: () => void
  /** 읽음 처리하고 목록에서 뺀다. 스와이프로 지울 때 쓴다 */
  markRead: (id: number) => Promise<void>
  /** 마지막으로 실패한 요청의 사유 */
  error: string | null
  clearError: () => void
}

export const NotificationContext = createContext<NotificationValue | null>(null)
