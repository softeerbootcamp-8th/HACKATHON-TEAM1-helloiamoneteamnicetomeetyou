import { useCallback } from 'react'

import { useNotification } from '@/features/notification/useNotification'
import { cancelExchange, fetchExchange } from '@/lib/exchange'

import { getDeviceId } from './identity'
import { activeAppointment } from './reducer'
import { useStore } from './useStore'

/**
 * 거래를 취소한다. 취소가 실제로 됐으면 `true` 를 돌려준다.
 *
 * **상대가 먼저 "만났어요" 를 눌렀으면 취소되지 않는다.** 두 사람이 서로 다른 버튼을 누를 수
 * 있어서, 먼저 도착한 쪽만 반영되고 늦은 쪽은 그 결과를 따라야 한다. 그때는 서버에서 현재 상태를
 * 읽어 화면을 맞추고 `false` 를 돌려준다. 부르는 쪽은 그 값을 보고 홈으로 보낼지 정한다.
 *
 * 서버에 닿지 못한 경우에는 화면을 접는다. 취소하겠다고 누른 사람을 네트워크 문제로 붙잡아 두는
 * 것보다는 낫다. 그 경우 상대에게는 전해지지 않고, 상대는 시간 조율이나 취소로 풀게 된다.
 *
 * <b>알림함을 다시 읽는다.</b> 서버는 교환이 끝나면 그 교환의 알림을 읽음 처리하는데, 취소를
 * 누른 본인에게는 실시간 알림이 가지 않아서(`notifyOthers`) 다시 읽지 않으면 방금 정리된
 * 알림이 화면에만 남는다.
 */
export function useCancelAppointment() {
  const { state, dispatch } = useStore()
  const { refresh: refreshNotifications } = useNotification()
  const exchangeId = activeAppointment(state)?.exchangeId

  return useCallback(async (): Promise<boolean> => {
    if (exchangeId === undefined) {
      dispatch({ type: 'cancel-appointment' })
      return true
    }

    const myUserId = getDeviceId()

    try {
      await cancelExchange(exchangeId, myUserId)
      dispatch({ type: 'cancel-appointment' })
      refreshNotifications()
      return true
    } catch {
      const latest = await fetchExchange(exchangeId).catch(() => null)

      // 이미 끝난 약속이면 서버가 맞다. 화면을 그 상태로 맞추고 취소는 없던 일로 둔다.
      if (latest && latest.status !== 'PENDING' && latest.status !== 'IN_PROGRESS') {
        dispatch({ type: 'exchange-synced', exchange: latest, myUserId })
        dispatch({
          type: 'toast',
          message:
            latest.status === 'COMPLETED'
              ? '상대가 교환을 마쳤다고 했어요'
              : '이미 취소된 약속이에요',
        })
        return false
      }

      dispatch({ type: 'cancel-appointment' })
      return true
    }
  }, [exchangeId, dispatch, refreshNotifications])
}
