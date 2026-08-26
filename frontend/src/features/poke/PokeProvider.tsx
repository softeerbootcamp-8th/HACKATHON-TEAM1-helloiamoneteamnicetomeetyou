import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'

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
  const [error, setError] = useState<string | null>(null)
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
        // 셋을 나란히 읽는다. 서로 기다릴 이유가 없고, 화면은 세 개를 같이 쓴다.
        const [receivedPage, sentPage, waitingPage] = await Promise.all([
          fetchReceivedPokes(userId, signal),
          fetchSentPokes(userId, signal),
          fetchBoothHaveItems(boothId, userId, signal),
        ])
        if (signal.aborted) return

        setReceived(receivedPage.content)
        setSent(sentPage.content)
        setWaiting(waitingPage.content)
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
  })

  // 요청이 겹치는 것을 막는다. 버튼을 두 번 누르면 서버가 4011 로 막지만, 그 전에
  // 화면에서 걸러 주는 편이 사용자에게 덜 놀랍다.
  const busy = useRef(false)

  const run = useCallback(
    async (action: () => Promise<void>) => {
      if (busy.current) return
      busy.current = true
      setError(null)
      try {
        await action()
        refresh()
      } catch (err) {
        setError(messageOf(err))
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
      run(async () => {
        await answerPoke(pokeId, userId, 'ACCEPTED', chosenItemId)
      }),
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
      ready,
      refresh,
      send,
      accept,
      reject,
      error,
      clearError,
    }),
    [received, sent, waiting, ready, refresh, send, accept, reject, error, clearError],
  )

  return <PokeContext.Provider value={value}>{children}</PokeContext.Provider>
}
