import { useCallback, useEffect, useMemo, useReducer, type ReactNode } from 'react'

import { fetchBooths, fetchExchange, fetchZones, registerUser } from '@/lib/exchange'
import { useBoothEvents } from '@/lib/use-booth-events'
import { ALL_WAITING, myUsername } from '@/mocks/data'

import { StoreContext } from './context'
import { getDeviceId } from './identity'
import { initialState, reducer } from './reducer'

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState)
  const myUserId = useMemo(() => getDeviceId(), [])

  /*
    앱을 열면 나를 등록하고 부스를 읽는다. 등록은 멱등이라 매번 불러도 되고, 부스 id 가 있어야
    실시간 알림을 구독할 수 있다.

    실패해도 화면은 그대로 돈다. 서버가 없어도 목업 흐름은 볼 수 있어야 해서, 여기서 막지 않고
    붙는 화면들이 각자 처리한다.
  */
  useEffect(() => {
    let cancelled = false

    const load = async () => {
      try {
        await registerUser(myUserId, myUsername(myUserId))
        const booths = await fetchBooths()
        const booth = booths[0]
        if (!booth || cancelled) return

        const zones = await fetchZones(booth.id)
        if (cancelled) return

        dispatch({ type: 'booth-loaded', boothId: booth.id, zones })
      } catch {
        // 서버가 아직 없거나 꺼져 있는 경우다. 약속 화면에서 다시 알린다.
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [myUserId])

  /**
   * 실시간 알림을 받으면 약속을 서버에서 다시 읽는다.
   *
   * 이벤트에 담긴 상태를 그대로 쓰지 않는 것이 중요하다. 끊겼던 동안의 이벤트는 다시 오지 않아서,
   * 매번 현재 상태를 읽어야 재연결 뒤에도 화면이 확실히 맞는다. 이벤트에서 꺼내 쓰는 것은
   * "어느 약속을 읽어야 하는지" 하나뿐이다.
   *
   * **그 id 가 꼭 필요하다.** 상대가 교환을 만들면 내 화면에는 아직 약속이 없어서, 이벤트가 없으면
   * 무엇을 읽어야 할지 알 수 없다. `CONNECTED` 처럼 id 가 없는 알림일 때만 들고 있던 약속을 쓴다.
   */
  const syncExchange = useCallback(
    async (data?: unknown) => {
      const exchangeId = exchangeIdOf(data) ?? state.appointment?.exchangeId
      if (exchangeId === undefined) return

      try {
        const exchange = await fetchExchange(exchangeId)
        dispatch({ type: 'exchange-synced', exchange, myUserId })
      } catch {
        // 잠깐 실패한 것이면 다음 알림이나 heartbeat 재연결 때 다시 맞는다.
      }
    },
    [state.appointment?.exchangeId, myUserId],
  )

  /*
    화면 전환과 무관하게 연결이 유지된다. StoreProvider 는 라우터보다 위에 있어서 앱이 살아 있는
    동안 언마운트되지 않고, 구독 단위가 부스라 지도에서 자리를 옮겨도 다시 붙을 일이 없다.

    연결은 사람당 하나면 된다. 서버가 연결을 부스별과 사용자별 두 벌로 색인해 두고, 약속 알림은
    그 교환의 참가자에게만 보내기 때문에 약속마다 따로 연결할 이유가 없다.
  */
  useBoothEvents(state.boothId, myUserId, {
    CONNECTED: () => void syncExchange(),
    EXCHANGE_CREATED: (data) => void syncExchange(data),
    EXCHANGE_TIME_UPDATED: (data) => void syncExchange(data),
    EXCHANGE_ARRIVED: (data) => void syncExchange(data),
    EXCHANGE_CANCELLED: (data) => void syncExchange(data),
  })

  // 자동 매칭. 켜져 있는 동안 짧은 간격으로 상대를 찾는다.
  useEffect(() => {
    if (!state.autoMatching) return
    const timer = window.setTimeout(() => dispatch({ type: 'auto-match-tick' }), 2600)
    return () => window.clearTimeout(timer)
  }, [state.autoMatching, state.have, state.needs])

  // 매칭이 끝나면 붙잡아 둔 찔러보기를 그때 알린다.
  useEffect(() => {
    if (state.autoMatching || !state.heldIncoming) return
    const timer = window.setTimeout(() => dispatch({ type: 'release-held-poke' }), 700)
    return () => window.clearTimeout(timer)
  }, [state.autoMatching, state.heldIncoming])

  // 보낸 찔러보기에 대한 상대의 응답.
  useEffect(() => {
    if (state.outgoingPoke?.status !== 'pending') return
    const timer = window.setTimeout(() => {
      // 상대가 내 묶음에서 원하는 카드를 찾을 수 있으면 수락한다.
      const target = ALL_WAITING.find((u) => u.id === state.outgoingPoke?.targetUserId)
      const accepted = Boolean(
        target && state.have.some((s) => target.needsItemIds.includes(s.itemId)),
      )
      dispatch({ type: 'poke-answered', accepted })
    }, 3200)
    return () => window.clearTimeout(timer)
  }, [state.outgoingPoke, state.have])

  // 남이 나에게 보내는 찔러보기. 내놓은 카드가 있어야 들어온다.
  useEffect(() => {
    if (state.have.length === 0) return
    if (state.incomingPoke || state.heldIncoming || state.match || state.appointment) return
    const timer = window.setTimeout(() => {
      const mine = state.have[0].itemId
      const from = ALL_WAITING.find(
        (u) => u.needsItemIds.includes(mine) && u.id !== state.outgoingPoke?.targetUserId,
      )
      if (!from) return
      const offered = ALL_WAITING.filter((u) => u.id !== from.id)
        .map((u) => u.itemId)
        .filter((id, i, arr) => arr.indexOf(id) === i)
        .slice(0, 3)
      dispatch({
        type: 'receive-poke',
        poke: {
          fromUserId: from.id,
          wantItemId: mine,
          offeredItemIds: [from.itemId, ...offered].slice(0, 3),
        },
      })
    }, 9000)
    return () => window.clearTimeout(timer)
  }, [
    state.have,
    state.incomingPoke,
    state.heldIncoming,
    state.match,
    state.appointment,
    state.outgoingPoke,
  ])

  // 토스트는 스스로 사라진다.
  useEffect(() => {
    if (!state.toast) return
    const timer = window.setTimeout(() => dispatch({ type: 'toast', message: null }), 2400)
    return () => window.clearTimeout(timer)
  }, [state.toast])

  const value = useMemo(() => ({ state, dispatch }), [state])
  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}

/** 알림에 실려 온 약속 id. 서버가 `{ "exchangeId": 1 }` 만 담아 보낸다. */
function exchangeIdOf(data: unknown): number | undefined {
  if (typeof data !== 'object' || data === null || !('exchangeId' in data)) return undefined

  const exchangeId = (data as { exchangeId: unknown }).exchangeId
  return typeof exchangeId === 'number' ? exchangeId : undefined
}
