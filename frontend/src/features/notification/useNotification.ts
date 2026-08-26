import { useContext } from 'react'

import { NotificationContext, type NotificationValue } from './NotificationContext'

export function useNotification(): NotificationValue {
  const value = useContext(NotificationContext)
  if (!value) throw new Error('useNotification 은 NotificationProvider 안에서만 쓸 수 있습니다')
  return value
}
