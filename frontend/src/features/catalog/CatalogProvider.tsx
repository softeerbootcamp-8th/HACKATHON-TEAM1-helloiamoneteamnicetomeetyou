import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react'

import { messageOf } from '@/lib/api'
import { ALL_ITEMS, myUsername } from '@/mocks/data'
import { getDeviceId } from '@/store/identity'

import { fetchBoothItems, fetchBooths, registerUser } from './api'
import { CatalogContext, type CatalogState } from './CatalogContext'
import { matchByName } from './match-by-name'

/**
 * 앱이 열릴 때 서버와 한 번 맞춰 둔다. 카드 등록 화면이 이 결과를 쓴다.
 *
 * 하는 일 세 가지다.
 * 1. 이 기기를 서버에 등록한다 (`POST /api/users`). 안 하면 카드 등록이 전부 막힌다
 * 2. 부스 목록에서 첫 부스를 고른다. 부스는 어드민에서 만들어 id 가 고정이 아니다
 * 3. 그 부스의 카드를 받아 목업 카드와 이름으로 잇는다
 *
 * 셋 중 하나라도 안 되면 카드 등록만 막히고 나머지 화면(매칭, 레이더, 찔러보기)은 목업으로
 * 계속 돈다. 그쪽 API 가 아직 없어서, 여기서 앱 전체를 세우면 볼 수 있는 것도 못 보게 된다.
 */
export function CatalogProvider({ children }: { children: ReactNode }) {
  const userId = useMemo(() => getDeviceId(), [])
  const [state, setState] = useState<CatalogState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  const reload = useCallback(() => {
    setState({ status: 'loading' })
    setAttempt((n) => n + 1)
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller

    async function load() {
      try {
        await registerUser(userId, myUsername(userId), signal)

        const booths = await fetchBooths(signal)
        if (booths.length === 0) {
          setState({ status: 'empty', reason: '아직 열린 부스가 없습니다.' })
          return
        }

        const booth = booths[0]
        const serverItems = await fetchBoothItems(booth.id, signal)
        if (serverItems.length === 0) {
          setState({ status: 'empty', reason: `${booth.name} 에 등록된 카드가 아직 없습니다.` })
          return
        }

        const { serverIdOf, unmatched } = matchByName(ALL_ITEMS, serverItems)
        setState({ status: 'ready', boothId: booth.id, serverIdOf, unmatched })
      } catch (error) {
        if (signal.aborted) return
        setState({ status: 'error', reason: messageOf(error) })
      }
    }

    void load()

    // StrictMode 가 개발 모드에서 effect 를 두 번 돌린다. 먼저 뜬 요청은 여기서 끊는다.
    return () => controller.abort()
  }, [userId, attempt])

  const value = useMemo(() => ({ state, userId, reload }), [state, userId, reload])

  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>
}
