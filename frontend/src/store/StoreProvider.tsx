import { useCallback, useEffect, useMemo, useReducer, type ReactNode } from 'react'

import { fetchMyHaveItems, fetchMyWantItems, type RegisteredItem } from '@/features/catalog/api'
import { useCatalog } from '@/features/catalog/useCatalog'
import { fetchActiveExchange, fetchExchange } from '@/lib/exchange'
import { useBoothEvents } from '@/lib/use-booth-events'

import { StoreContext } from './context'
import { initialState, reducer } from './reducer'

/** 시안의 `토스트 정리` 가 정한 노출 시간이다. 5초 뒤에 스스로 사라진다. */
const TOAST_MS = 5000

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState)
  const { state: catalog, userId: myUserId } = useCatalog()

  /*
    부스는 CatalogProvider 가 이미 골라 놨다. 부스 id 를 넣어 두고(실시간 구독이 이걸 쓴다),
    진행 중인 약속이 있으면 그 자리로 돌아온다. 새로고침하면 화면 상태가 통째로 사라지기
    때문에 필요하다.

    만나는 자리는 따로 읽지 않는다. 자리는 교환마다 서버가 정해서 `Exchange.zone` 으로 같이
    내려주고, 화면은 그것만 보여준다.

    카탈로그가 준비되기 전에는 아무것도 하지 않는다. 부스를 모르면 읽을 것도 없다.
  */
  const boothId = catalog.status === 'ready' ? catalog.boothId : null

  useEffect(() => {
    if (boothId === null) return

    let cancelled = false

    const load = async () => {
      try {
        dispatch({ type: 'booth-loaded', boothId })

        const active = await fetchActiveExchange(myUserId)
        if (active && !cancelled) {
          dispatch({ type: 'exchange-synced', exchange: active, myUserId, activate: true })
        }
      } catch {
        // 약속 화면에서 다시 알린다. 여기서 앱을 세우지 않는다.
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [boothId, myUserId])

  /*
    서버에 이미 등록해 둔 카드를 화면으로 되살린다.

    새로고침하면 화면 상태가 통째로 사라지는데 서버 등록은 그대로 남는다. 그 상태로 등록
    화면에 들어가면 아무것도 안 고른 것처럼 보이고, "교환하러 가기" 를 누르는 순간 등록
    동기화가 "이번 선택에 없는 카드" 로 보고 서버에 있던 것을 전부 해제한다.

    <b>지금 부스에 있는 카드만 되살린다.</b> 등록 조회는 부스를 가리지 않고 내 등록 전부를
    돌려주는데, 부스를 옮기면 그 목록에 앞 부스 카드가 섞여 있다. 그대로 넣으면 부스를 옮길 때
    화면을 비우는 처리(`Onboarding` 의 `pickBooth`)가 무의미해지고, 지금 부스에서는 고를 수도
    없는 카드가 개수에만 잡힌다.

    실패하면 조용히 넘어간다. 되살리지 못한 것뿐이라 화면을 세울 이유가 없다.
  */
  useEffect(() => {
    if (catalog.status !== 'ready') return

    const { itemById } = catalog
    const controller = new AbortController()
    const { signal } = controller

    void (async () => {
      try {
        const [have, needs] = await Promise.all([
          fetchMyHaveItems(myUserId, signal),
          fetchMyWantItems(myUserId, signal),
        ])
        if (signal.aborted) return

        // qty 가 0 이하인 Selection 은 이 앱에서 존재하지 않는 상태다 (setQty/bump 가 0 이하면
        // 아예 목록에서 뺀다). 지금 새로 낼 게 없는(quantity_left = 0, 예약되었거나 다 나간)
        // 카드까지 그대로 넣으면 "가지고 있다는데 수량이 0" 으로 화면이 그 규칙을 어기게 된다.
        const inThisBooth = (rows: RegisteredItem[]) =>
          rows
            .filter((row) => row.quantity > 0 && itemById(row.itemId) !== undefined)
            .map((row) => ({ itemId: row.itemId, qty: row.quantity }))

        dispatch({
          type: 'registrations-loaded',
          have: inThisBooth(have),
          needs: inThisBooth(needs),
        })
      } catch {
        // 되살리지 못했을 뿐이다. 고르는 것부터 다시 하면 된다.
      }
    })()

    return () => controller.abort()
  }, [catalog, myUserId])

  /**
   * 실시간 알림을 받으면 약속을 서버에서 다시 읽는다.
   *
   * 이벤트에 담긴 상태를 그대로 쓰지 않는다. 끊겼던 동안의 이벤트는 다시 오지 않아서, 매번
   * 현재 상태를 읽어야 재연결 뒤에도 화면이 확실히 맞는다. 이벤트에서 꺼내 쓰는 것은
   * "어느 약속을 읽어야 하는지" 하나뿐이다.
   *
   * **그 id 가 꼭 필요하다.** 상대가 교환을 만들면 내 화면에는 아직 약속이 없어서, 이벤트가
   * 없으면 무엇을 읽어야 할지 알 수 없다. `CONNECTED` 처럼 id 가 없는 알림일 때는 내가 지금
   * 잡고 있는 약속을 물어본다.
   */
  const syncExchange = useCallback(
    async (data?: unknown) => {
      try {
        const exchangeId = exchangeIdOf(data)
        const exchange =
          exchangeId === undefined
            ? await fetchActiveExchange(myUserId)
            : await fetchExchange(exchangeId)

        if (exchange) {
          dispatch({ type: 'exchange-synced', exchange, myUserId })
        }
      } catch {
        // 잠깐 실패한 것이면 다음 알림이나 heartbeat 재연결 때 다시 맞는다.
      }
    },
    [myUserId],
  )

  /*
    화면 전환과 무관하게 연결이 유지된다. StoreProvider 는 라우터보다 위에 있어서 앱이 살아 있는
    동안 언마운트되지 않고, 구독 단위가 부스라 지도에서 자리를 옮겨도 다시 붙을 일이 없다.

    연결은 사람당 하나면 된다. 서버가 연결을 부스별과 사용자별 두 벌로 색인해 두고, 약속 알림은
    그 교환의 참가자에게만 보내기 때문에 약속마다 따로 연결할 이유가 없다.
  */
  useBoothEvents(boothId, myUserId, {
    CONNECTED: () => void syncExchange(),
    EXCHANGE_CREATED: (data) => void syncExchange(data),
    EXCHANGE_TIME_REQUESTED: (data) => void syncExchange(data),
    EXCHANGE_TIME_MATCHED: (data) => void syncExchange(data),
    EXCHANGE_TIME_MISMATCHED: (data) => void syncExchange(data),
    EXCHANGE_TIME_AGREED: (data) => void syncExchange(data),
    EXCHANGE_TIME_UPDATED: (data) => void syncExchange(data),
    EXCHANGE_SLOTS_UPDATED: (data) => void syncExchange(data),
    EXCHANGE_PLACE_UPDATED: (data) => void syncExchange(data),
    EXCHANGE_ARRIVED: (data) => void syncExchange(data),
    EXCHANGE_COMPLETED: (data) => void syncExchange(data),
    EXCHANGE_CANCELLED: (data) => void syncExchange(data),
  })

  // 토스트는 스스로 사라진다.
  useEffect(() => {
    if (!state.toast) return
    const timer = window.setTimeout(() => dispatch({ type: 'toast', message: null }), TOAST_MS)
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
