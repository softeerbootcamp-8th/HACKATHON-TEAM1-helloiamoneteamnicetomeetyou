import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'

import { fetchMyHaveItems, type RegisteredItem } from '@/features/catalog/api'
import { useCatalog } from '@/features/catalog/useCatalog'
import { messageOf } from '@/lib/api'
import { useBoothEvents } from '@/lib/use-booth-events'

import {
  answerPoke,
  fetchBoothHaveItems,
  fetchReceivedPokes,
  fetchSentPokes,
  sendPoke,
  type BoothHaveItem,
  type ReceivedPoke,
  type SentPoke,
} from './api'
import { PokeContext } from './PokeContext'

/**
 * 서버에 오간 찔러보기를 들고 있고, 실시간 알림을 받으면 다시 읽는다.
 *
 * <b>알림 내용으로 화면을 고치지 않고 목록을 다시 읽는다.</b> 서버가 끊긴 동안의 이벤트를
 * 재전송하지 않기 때문에, 이벤트를 하나씩 반영하는 방식으로는 연결이 한 번 끊기면 화면이
 * 옛 상태로 남는다. `CONNECTED` 에서도 같은 일을 하는 것이 그래서다.
 */
export function PokeProvider({ children }: { children: ReactNode }) {
  const { state: catalog, userId } = useCatalog()

  const [received, setReceived] = useState<ReceivedPoke[]>([])
  const [sent, setSent] = useState<SentPoke[]>([])
  const [waiting, setWaiting] = useState<BoothHaveItem[]>([])
  const [myOfferable, setMyOfferable] = useState<RegisteredItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loaded, setLoaded] = useState(false)
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
        // 넷을 나란히 읽는다. 서로 기다릴 이유가 없고, 화면은 넷을 같이 쓴다.
        const [receivedPage, sentPage, waitingPage, myHave] = await Promise.all([
          fetchReceivedPokes(userId, signal),
          fetchSentPokes(userId, signal),
          fetchBoothHaveItems(boothId, userId, signal),
          fetchMyHaveItems(userId, signal),
        ])
        if (signal.aborted) return

        setReceived(receivedPage.content)
        setSent(sentPage.content)
        setWaiting(waitingPage.content)
        // 서버가 상대에게 묶음을 보여줄 때 쓰는 것과 같은 규칙으로 거른다
        // (`PokeService.offerableItems`). 응답의 quantity 는 지금 새로 내줄 수 있는 개수라
        // 다른 교환에 예약된 만큼은 이미 빠져 있고, 0 이면 상대가 고를 수 없는 카드다.
        // 부스로 거르지 않는 것도 서버와 맞추기 위해서다.
        setMyOfferable(myHave.filter((row) => row.quantity > 0))
        setLoaded(true)
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
    POKE_RECEIVED: onBoothEvent,
    POKE_ACCEPTED: onBoothEvent,
    POKE_REJECTED: onBoothEvent,
    // 부스에 누가 카드를 새로 등록했다. 그 사람이 레이더와 전체 리스트에 나타나야 한다.
    //
    // 각 줄의 "내 희망 카드인가" 와 "내가 줄 수 있는 카드" 는 보는 사람마다 다르게 계산되는
    // 값이라 이벤트에 담아 뿌릴 수 없다. 그래서 신호만 받고 목록을 다시 읽는다.
    USER_JOINED: onBoothEvent,
    USER_LEFT: onBoothEvent,
    // 교환이 잡히면 내 카드가 그만큼 예약되고, 끝나거나 취소되면 돌아온다. 즉 상대가 내 묶음에서
    // 고를 수 있는 카드가 이 순간들에 바뀐다. 찔러보기 확인 화면을 열어 둔 채로 자동 매칭이
    // 성사되면, 다시 읽지 않는 한 이미 나간 카드를 계속 내주겠다고 보여주게 된다.
    MATCH_ACCEPTED: onBoothEvent,
    EXCHANGE_CREATED: onBoothEvent,
    EXCHANGE_COMPLETED: onBoothEvent,
    EXCHANGE_CANCELLED: onBoothEvent,
  })

  // 요청이 겹치는 것을 막는다. 버튼을 두 번 누르면 서버가 4011 로 막지만, 그 전에
  // 화면에서 걸러 주는 편이 사용자에게 덜 놀랍다.
  const busy = useRef(false)

  const run = useCallback(
    async <T,>(action: () => Promise<T>): Promise<T> => {
      // 겹친 요청은 앞선 것이 끝나기를 기다리지 않고 그 자리에서 접는다. 호출한 쪽이
      // 결과를 쓰기 때문에, 조용히 undefined 를 돌려주면 화면이 빈 값으로 그려진다.
      if (busy.current) throw new Error('이미 처리 중입니다')
      busy.current = true
      setError(null)
      try {
        const result = await action()
        refresh()
        return result
      } catch (err) {
        setError(messageOf(err))
        // 실패는 대개 그 사이 서버가 달라졌다는 뜻이다 — 조르려던 카드가 다 나갔거나, 내가
        // 내줄 카드가 없어졌거나, 이미 보낸 뒤거나. 다시 읽어야 화면이 사실과 맞는다.
        refresh()
        throw err
      } finally {
        busy.current = false
      }
    },
    [refresh],
  )

  const send = useCallback(
    (targetUserId: string, requestedItemId: number) =>
      run(async () => {
        await sendPoke(userId, targetUserId, requestedItemId)
      }),
    [run, userId],
  )

  const accept = useCallback(
    (pokeId: number, chosenItemId: number) =>
      run(() => answerPoke(pokeId, userId, 'ACCEPTED', chosenItemId)),
    [run, userId],
  )

  const reject = useCallback(
    (pokeId: number) =>
      run(async () => {
        await answerPoke(pokeId, userId, 'REJECTED')
      }),
    [run, userId],
  )

  const value = useMemo(
    () => ({
      received,
      sent,
      waiting,
      myOfferable,
      ready,
      loaded,
      refresh,
      send,
      accept,
      reject,
      error,
      clearError,
    }),
    [
      received,
      sent,
      waiting,
      myOfferable,
      ready,
      loaded,
      refresh,
      send,
      accept,
      reject,
      error,
      clearError,
    ],
  )

  return <PokeContext.Provider value={value}>{children}</PokeContext.Provider>
}
