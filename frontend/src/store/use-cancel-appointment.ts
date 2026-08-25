import { useCallback } from 'react'

import { cancelExchange } from '@/lib/exchange'

import { getDeviceId } from './identity'
import { useStore } from './useStore'

/**
 * 거래를 취소한다. 화면을 먼저 접고 서버에 알린다.
 *
 * **서버에 알리는 것이 중요하다.** 내 화면에서만 사라지면 상대는 오지 않을 사람을 계속 기다린다.
 * 상대 화면에는 `EXCHANGE_CANCELLED` 가 실시간으로 도착한다.
 *
 * 서버 호출이 실패해도 화면은 접는다. 취소하겠다고 누른 사람을 실패했다며 붙잡아 두는 것보다는
 * 낫다. 그 경우 상대에게는 전해지지 않고, 상대는 시간 조율로 풀게 된다.
 */
export function useCancelAppointment() {
  const { state, dispatch } = useStore()
  const exchangeId = state.appointment?.exchangeId

  return useCallback(async () => {
    dispatch({ type: 'cancel-appointment' })

    if (exchangeId === undefined) return

    try {
      await cancelExchange(exchangeId, getDeviceId())
    } catch {
      // 화면은 이미 접혔다. 상대에게 전해지지 않았을 뿐이다.
    }
  }, [exchangeId, dispatch])
}
