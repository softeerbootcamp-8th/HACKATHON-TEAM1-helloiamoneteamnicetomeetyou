import { useCallback, useState } from 'react'

import { messageOf } from '@/lib/api'
import type { Selection } from '@/store/types'

import {
  fetchMyHaveItems,
  fetchMyWantItems,
  registerHaveItem,
  registerWantItem,
  removeHaveItem,
  removeWantItem,
} from './api'
import type { CatalogState } from './CatalogContext'
import { useCatalog } from './useCatalog'

/**
 * 고른 카드를 서버에 등록하고, 끝나면 다음 화면으로 넘긴다.
 *
 * **내놓을 카드와 찾는 카드를 한 번에 보낸다.** 예전에는 Have 화면의 "다음" 이 곧바로
 * `/api/have-items` 를 불러서, 찾는 굿즈를 고르다 그만두거나 뒤로 가서 고쳐도 서버에는 이미
 * 등록이 남았다. 두 화면을 다 지나 "교환하러 가기" 를 눌렀을 때만 보낸다.
 *
 * **화면에서 뺀 카드는 서버에서도 지운다.** 고른 것만 보내면 서버는 "이번에 안 온 카드" 를 알
 * 방법이 없어서, 한 번 등록한 카드가 영원히 남아 매칭에 계속 잡힌다. 그래서 보내기 전에 서버가
 * 들고 있는 목록을 먼저 읽고 차집합을 지운다. 화면 상태는 새로고침에 사라지므로 이전 선택을
 * 화면 기억에서 꺼낼 수는 없다. 서버가 유일한 기준이다.
 *
 * 고른 카드의 `itemId` 가 곧 서버 카드 id 다. 등록 화면이 서버 목록을 그대로 그리기 때문에
 * 여기서 바꿔 줄 것이 없다.
 */
export function useRegisterSelections() {
  const { state, userId } = useCatalog()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | undefined>(undefined)

  const submit = useCallback(
    async (have: Selection[], needs: Selection[], done: () => void) => {
      if (state.status !== 'ready') return

      setSubmitting(true)
      setError(undefined)

      try {
        const [serverHave, serverWant] = await Promise.all([
          fetchMyHaveItems(userId),
          fetchMyWantItems(userId),
        ])

        const goneFrom = (registered: { itemId: number }[], selections: Selection[]) => {
          const keep = new Set(selections.map((s) => s.itemId))
          return registered.map((r) => r.itemId).filter((itemId) => !keep.has(itemId))
        }

        // 지우는 것이 먼저다. 같은 카드를 have 에서 want 로 옮기는 경우, 예전 등록이 남은 채로
        // 새 등록을 보내면 서버의 상호 배제 검증에 막힌다.
        await Promise.all(
          goneFrom(serverWant, needs).map((itemId) => removeWantItem(userId, itemId)),
        )
        await Promise.all(
          goneFrom(serverHave, have).map((itemId) => removeHaveItem(userId, itemId)),
        )

        // have 를 먼저 보낸다. 서버가 등록마다 매칭을 다시 돌리는데, 내놓을 카드가 아직 다 안
        // 들어간 상태에서 찾는 카드가 먼저 들어가면 덜 채워진 상태로 매칭이 성사될 수 있다.
        await Promise.all(have.map((s) => registerHaveItem(userId, s.itemId, s.qty)))
        await Promise.all(needs.map((s) => registerWantItem(userId, s.itemId, s.qty)))
        done()
      } catch (e) {
        setError(messageOf(e))
      } finally {
        setSubmitting(false)
      }
    },
    [state.status, userId],
  )

  return { submit, submitting, error }
}

/**
 * 카드를 그릴 수 없을 때 화면에 띄울 한 줄.
 *
 * 무엇이 막혔는지 말해 주지 않으면 "분명 골랐는데 아무 일도 안 일어난다" 가 된다. 전에는
 * 여기서 "고른 것은 이 기기에만 저장됩니다" 라고 알렸는데, 이제 서버 카드를 못 받으면 고를
 * 것 자체가 없어서 화면이 사유만 보여주고 멈춘다.
 */
export function catalogNotice(state: CatalogState): string | undefined {
  if (state.status === 'loading') return '카드 목록을 받아오는 중입니다.'
  if (state.status === 'empty') return state.reason
  if (state.status === 'error') return `${state.reason} 잠시 뒤에 다시 시도해 주세요.`
  return undefined
}
