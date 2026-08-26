import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react'

import { messageOf } from '@/lib/api'
import { getDeviceId, myUsername } from '@/store/identity'

import { fetchBoothItems, fetchBooths, registerUser, type ServerBooth } from './api'
import { CatalogContext, type CatalogState } from './CatalogContext'
import { forgetBoothId, readSavedBoothId, saveBoothId } from './saved-booth'

/**
 * 앱이 열릴 때 서버와 한 번 맞춰 둔다. 카드 등록 화면이 이 결과를 쓴다.
 *
 * 하는 일 세 가지다.
 * 1. 이 기기를 서버에 등록한다 (`POST /api/users`). 안 하면 카드 등록이 전부 막힌다
 * 2. 부스 목록을 받아 붙을 부스를 정한다. 부스는 어드민에서 만들어 id 가 고정이 아니다
 * 3. 그 부스의 카드를 받아 둔다. 등록 화면부터 매칭 결과까지 전부 이 목록을 그린다
 *
 * 셋 중 하나라도 안 되면 카드를 그릴 수 없어서 등록 화면이 사유를 띄우고 멈춘다. 전에는
 * 목업 카드로 떨어졌는데, 서버에 없는 카드를 고르게 두면 등록도 매칭도 안 되는 채로 흐름만
 * 계속 타서 무엇이 잘못됐는지 알 수 없게 된다.
 *
 * **부스 목록과 카드 목록을 두 effect 로 나눠 둔다.** 부스를 바꿀 때 다시 받아야 하는 것은
 * 카드뿐이다. 한 덩어리로 두면 부스를 고를 때마다 부스 목록까지 다시 받게 된다.
 */
export function CatalogProvider({ children }: { children: ReactNode }) {
  const userId = useMemo(() => getDeviceId(), [])
  const [state, setState] = useState<CatalogState>({ status: 'loading' })
  const [booths, setBooths] = useState<ServerBooth[]>([])
  /**
   * 붙어 있는 부스를 id 가 아니라 통째로 들고 있는다. 카드를 받아 오는 쪽이 실패 문구에
   * 부스 이름을 써야 하는데, id 만 들고 있으면 그 이름을 찾으려고 `booths` 를 다시 참조하게
   * 되고 그러면 부스 목록이 갱신될 때마다 카드까지 다시 받는다.
   */
  const [booth, setBooth] = useState<ServerBooth | null>(null)
  const [attempt, setAttempt] = useState(0)

  const reload = useCallback(() => {
    setState({ status: 'loading' })
    setAttempt((n) => n + 1)
  }, [])

  const selectBooth = useCallback(
    (boothId: number) => {
      const next = booths.find((b) => b.id === boothId)
      if (!next) return
      saveBoothId(next.id)
      setBooth(next)
    },
    [booths],
  )

  // 1. 기기를 등록하고 부스 목록을 받아 붙을 부스를 정한다.
  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller

    async function load() {
      try {
        await registerUser(userId, myUsername(userId), signal)

        const list = await fetchBooths(signal)
        if (signal.aborted) return

        setBooths(list)
        if (list.length === 0) {
          setBooth(null)
          setState({ status: 'empty', reason: '아직 열린 부스가 없습니다.' })
          return
        }

        // 지난번에 고른 부스가 아직 살아 있으면 그리로 돌아간다. 어드민에서 지웠거나 다른
        // 행사장이면 목록에 없으니, 저장해 둔 값을 지우고 첫 부스로 떨어진다.
        const saved = readSavedBoothId()
        const remembered = list.find((b) => b.id === saved)
        if (saved !== null && !remembered) forgetBoothId()

        setBooth(remembered ?? list[0])
      } catch (error) {
        if (signal.aborted) return
        setState({ status: 'error', reason: messageOf(error) })
      }
    }

    void load()

    // StrictMode 가 개발 모드에서 effect 를 두 번 돌린다. 먼저 뜬 요청은 여기서 끊는다.
    return () => controller.abort()
  }, [userId, attempt])

  // 2. 정해진 부스의 카드를 받는다. 부스를 바꾸면 여기만 다시 돈다.
  useEffect(() => {
    if (!booth) return

    const controller = new AbortController()
    const { signal } = controller

    async function load(target: ServerBooth) {
      setState({ status: 'loading' })
      try {
        const serverItems = await fetchBoothItems(target.id, signal)
        if (signal.aborted) return

        if (serverItems.length === 0) {
          setState({ status: 'empty', reason: `${target.name} 에 등록된 카드가 아직 없습니다.` })
          return
        }

        // 목록을 한 번 훑어 색인해 둔다. 카드를 그리는 자리마다 배열을 다시 뒤지면
        // 레이더와 목록이 매 렌더마다 카드 수만큼 선형 탐색을 돈다.
        const byId = new Map(serverItems.map((item) => [item.id, item]))

        setState({
          status: 'ready',
          boothId: target.id,
          items: serverItems,
          itemById: (itemId) => byId.get(itemId),
        })
      } catch (error) {
        if (signal.aborted) return
        setState({ status: 'error', reason: messageOf(error) })
      }
    }

    void load(booth)

    return () => controller.abort()
  }, [booth])

  const value = useMemo(
    () => ({ state, userId, reload, booths, booth, selectBooth }),
    [state, userId, reload, booths, booth, selectBooth],
  )

  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>
}
