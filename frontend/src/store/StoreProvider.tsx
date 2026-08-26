import { useCallback, useEffect, useMemo, useReducer, useRef, type ReactNode } from 'react'

import { fetchMyHaveItems, fetchMyWantItems, type RegisteredItem } from '@/features/catalog/api'
import { useCatalog } from '@/features/catalog/useCatalog'
import { fetchActiveExchange, fetchExchange, fetchZones } from '@/lib/exchange'
import { useBoothEvents } from '@/lib/use-booth-events'

import { ALL_WAITING } from '@/mocks/data'

import { StoreContext } from './context'
import { initialState, reducer } from './reducer'
import type { Selection } from './types'

/** 시안의 `토스트 정리` 가 정한 노출 시간이다. 5초 뒤에 스스로 사라진다. */
const TOAST_MS = 5000

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState)
  const { state: catalog, userId: myUserId } = useCatalog()

  /*
    부스는 CatalogProvider 가 이미 골라 놨다. 그 부스의 교환 장소를 받아 두고, 진행 중인 약속이
    있으면 그 자리로 돌아온다. 새로고침하면 화면 상태가 통째로 사라지기 때문에 필요하다.

    카탈로그가 준비되기 전에는 아무것도 하지 않는다. 서버에 못 닿았으면 약속 화면만 막히고
    나머지는 목업으로 계속 돈다.
  */
  const boothId = catalog.status === 'ready' ? catalog.boothId : null

  // 내 카드를 서버에서 받아 채우는 것을 한 번만 시도했는지. 온보딩을 건너뛰고 홈이든
  // /have 든 어디로 먼저 들어오든, 새로고침 한 번에 한 번만 물으면 된다.
  const haveNeedsHydratedRef = useRef(false)

  useEffect(() => {
    if (boothId === null) return

    let cancelled = false

    const load = async () => {
      try {
        const zones = await fetchZones(boothId)
        if (cancelled) return
        dispatch({ type: 'booth-loaded', boothId, zones })

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

  /**
   * 온보딩을 건너뛰거나 /have, /needs 로 곧바로 들어온 사람의 카드를 서버에서 받아 채운다.
   *
   * `state.have`, `state.needs` 는 그 두 화면을 실제로 거쳐야만 채워지는 로컬 상태라, 새로고침
   * 한 번이면 서버에는 등록이 남아 있어도 화면은 빈 채로 시작한다. 이미 뭔가 골라 둔 상태를
   * 덮어쓰지 않도록, 아직 하나도 안 채워졌을 때 딱 한 번만 받는다.
   */
  useEffect(() => {
    if (catalog.status !== 'ready') return
    if (haveNeedsHydratedRef.current) return
    if (state.have.length > 0 || state.needs.length > 0) return
    haveNeedsHydratedRef.current = true

    const { mockIdOf } = catalog
    const controller = new AbortController()

    const toSelections = (rows: RegisteredItem[]): Selection[] =>
      rows
        .map((row) => ({ itemId: mockIdOf(row.itemId), qty: row.quantity }))
        .filter((s): s is Selection => s.itemId !== undefined)

    void (async () => {
      try {
        const [have, needs] = await Promise.all([
          fetchMyHaveItems(myUserId, controller.signal),
          fetchMyWantItems(myUserId, controller.signal),
        ])
        if (controller.signal.aborted) return
        dispatch({
          type: 'have-needs-hydrated',
          have: toSelections(have),
          needs: toSelections(needs),
        })
      } catch {
        // 못 받아도 화면은 뜬다. /have 에서 직접 고치면 그때 다시 맞는다.
      }
    })()

    return () => controller.abort()
  }, [catalog, myUserId, state.have.length, state.needs.length])

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
    EXCHANGE_TIME_UPDATED: (data) => void syncExchange(data),
    EXCHANGE_PLACE_UPDATED: (data) => void syncExchange(data),
    EXCHANGE_ARRIVED: (data) => void syncExchange(data),
    EXCHANGE_COMPLETED: (data) => void syncExchange(data),
    EXCHANGE_CANCELLED: (data) => void syncExchange(data),
  })

  // 지금 다루고 있는 약속. 아래 효과들이 이것만 보고 돌면 되므로 밖에서 한 번 꺼내 둔다.

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
      const target = ALL_WAITING.find((u) => u.id === state.outgoingPoke?.targetUserId)

      // 상대는 자기가 원하는 카드가 내 묶음에 있으면 그걸 고르고, 없으면 아무 한 장을
      // 고른다. 찔러보기는 원래 원하는 것이 없는 상대에게 보내는 것이라 뒤쪽이 보통이다.
      // 서버 연동에서는 이 자리에 실제로 고른 카드가 들어온다.
      const wanted = state.have.find((s) => target?.needsItemIds.includes(s.itemId))
      const chosen = wanted ?? state.have[0]

      dispatch({
        type: 'poke-answered',
        accepted: Boolean(target && chosen),
        chosenItemId: chosen?.itemId,
      })
    }, 3200)
    return () => window.clearTimeout(timer)
  }, [state.outgoingPoke, state.have])

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
