import { useCallback, useState } from 'react'

import { messageOf } from '@/lib/api'
import type { Selection } from '@/store/types'

import { registerHaveItem, registerWantItem } from './api'
import { useCatalog } from './useCatalog'

type Kind = 'have' | 'want'

const REGISTER: Record<Kind, typeof registerHaveItem> = {
  have: registerHaveItem,
  want: registerWantItem,
}

/**
 * 고른 카드를 서버에 등록하고, 끝나면 다음 화면으로 넘긴다.
 *
 * 서버가 아직 준비되지 않았으면(부스나 카드가 없거나 연결이 안 되면) **등록을 건너뛰고 그냥
 * 넘어간다.** 매칭과 레이더가 아직 목업으로 돌기 때문에, 여기서 막으면 등록 말고는 아무것도
 * 못 보게 된다. 대신 무엇이 안 됐는지는 화면에 남긴다.
 */
export function useRegisterSelections(kind: Kind) {
  const { state, userId } = useCatalog()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | undefined>(undefined)

  const submit = useCallback(
    async (selections: Selection[], done: () => void) => {
      if (state.status !== 'ready') {
        done()
        return
      }

      setSubmitting(true)
      setError(undefined)

      try {
        // 서버에 없는 카드는 보내지 않는다. 보내 봐야 404 다.
        const rows = selections
          .map((s) => ({ itemId: state.serverIdOf(s.itemId), qty: s.qty }))
          .filter((row): row is { itemId: number; qty: number } => row.itemId !== undefined)

        await Promise.all(rows.map((row) => REGISTER[kind](userId, row.itemId, row.qty)))
        done()
      } catch (e) {
        setError(messageOf(e))
      } finally {
        setSubmitting(false)
      }
    },
    [kind, state, userId],
  )

  return { submit, submitting, error }
}

/**
 * 서버가 준비되지 않았거나 일부 카드를 못 찾았을 때 화면 아래에 띄울 한 줄.
 *
 * 등록이 막힌 것을 사용자가 알아야 나중에 "분명 골랐는데 아무 일도 안 일어난다" 가 되지 않는다.
 */
export function catalogNotice(state: ReturnType<typeof useCatalog>['state']): string | undefined {
  if (state.status === 'loading') return '서버와 맞추는 중입니다.'
  if (state.status === 'empty') return `${state.reason} 고른 것은 이 기기에만 저장됩니다.`
  if (state.status === 'error') return `${state.reason} 고른 것은 이 기기에만 저장됩니다.`
  if (state.unmatched.length > 0) {
    const names = state.unmatched.map((i) => i.name).join(', ')
    return `${names} 은(는) 부스에 등록되지 않아 서버로 보내지 않습니다.`
  }
  return undefined
}
