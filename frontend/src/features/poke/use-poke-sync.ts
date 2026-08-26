import { useEffect, useRef } from 'react'

import { useStore } from '@/store/useStore'

import type { SentPoke } from './api'
import { usePoke } from './usePoke'

/**
 * 서버에 오간 찔러보기를 스토어에 이어 붙인다. 보낸 쪽에서 벌어지는 일을 맡는다.
 *
 * <b>`PokeProvider` 는 `StoreProvider` 바깥이라 스토어에 손댈 수 없다</b>(`main.tsx`).
 * 부스를 알아야 구독할 수 있어서 그 자리에 있는 것이고, 그래서 둘을 잇는 자리가 따로
 * 필요하다. `AppShell` 은 라우터 안쪽이라 둘을 다 볼 수 있고 화면이 바뀌어도 살아 있다.
 *
 * 여기서 실시간 알림을 따로 구독하지 않는다. `PokeProvider` 가 `POKE_ACCEPTED` 와
 * `POKE_REJECTED` 를 받을 때마다 목록을 다시 읽으므로, 보낸 목록이 바뀌는 것만 보면 된다.
 */
export function usePokeSync() {
  const { sent, loaded } = usePoke()
  const { dispatch } = useStore()

  /** 이미 성사 화면을 세워 준 교환. 목록을 다시 읽을 때마다 되살리지 않는다. */
  const seededExchanges = useRef(new Set<number>())
  /** 이미 알린 거절. `null` 이면 아직 첫 목록을 못 봤다는 뜻이다. */
  const toldRejections = useRef<Set<number> | null>(null)

  /**
   * 상대가 수락했다. 시안 `7. 찔러보기 성사` 를 세운다.
   *
   * 보낸 쪽 기준으로 내가 주는 카드는 상대가 내 묶음에서 고른 `chosenItem` 이고,
   * 받는 카드는 내가 조른 `requestedItem` 이다. 고른 카드는 서버만 아는 값이라
   * 화면이 짐작하면 늘 엉뚱한 카드를 집는다.
   *
   * 알림을 눌러 들어오든 새로고침 뒤에 주소로 들어오든 이 자리에서 세우기 때문에,
   * 성사 화면이 빈 화면으로 뜨지 않는다.
   */
  useEffect(() => {
    if (!loaded) return

    const accepted = latestOf(sent.filter((p) => p.status === 'ACCEPTED'))
    if (!accepted || accepted.exchangeId === undefined || !accepted.chosenItem) return
    if (seededExchanges.current.has(accepted.exchangeId)) return

    seededExchanges.current.add(accepted.exchangeId)
    dispatch({
      type: 'server-poke-matched',
      exchangeId: accepted.exchangeId,
      giveItemId: accepted.chosenItem.id,
      receiveItemId: accepted.requestedItem.id,
      partnerUserId: accepted.targetUserId,
      partnerName: accepted.targetUserName,
    })
  }, [loaded, sent, dispatch])

  /**
   * 상대가 거절했다. 시안 `토스트 정리`(204:5148)의 "거절된 경우" 문구를 띄운다.
   *
   * 알림함에도 한 줄 남지만(`PushMessage.POKE_REJECTED`), 그건 앱을 닫아 뒀을 때를
   * 위한 것이다. 보고 있는 사람에게는 토스트가 먼저 닿아야 한다.
   */
  useEffect(() => {
    if (!loaded) return

    const rejected = sent.filter((p) => p.status === 'REJECTED')

    // 앱을 켜기 전에 이미 거절된 건은 알리지 않는다. 첫 목록은 통째로 본 것으로 삼는다.
    if (toldRejections.current === null) {
      toldRejections.current = new Set(rejected.map((p) => p.pokeId))
      return
    }

    const told = toldRejections.current
    const fresh = rejected.filter((p) => !told.has(p.pokeId))
    if (fresh.length === 0) return
    for (const poke of fresh) told.add(poke.pokeId)

    // 여러 건이 한꺼번에 왔으면 가장 나중 것만 띄운다. 토스트는 한 번에 하나다.
    const last = latestOf(fresh)
    if (!last) return

    dispatch({
      type: 'toast',
      message: `${last.requestedItem.name} 교환이 거절되었어요`,
    })
  }, [loaded, sent, dispatch])
}

/** 가장 나중에 만들어진 것. 서버가 목록 순서를 약속하지 않아서 id 로 고른다. */
function latestOf(pokes: SentPoke[]): SentPoke | undefined {
  return pokes.reduce<SentPoke | undefined>(
    (latest, poke) => (latest === undefined || poke.pokeId > latest.pokeId ? poke : latest),
    undefined,
  )
}
