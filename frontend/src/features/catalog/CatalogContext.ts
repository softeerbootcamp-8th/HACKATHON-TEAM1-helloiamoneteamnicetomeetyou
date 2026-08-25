import { createContext } from 'react'

import type { Item } from '@/mocks/data'

/**
 * 서버 연동 준비 상태.
 *
 * - `loading`: 사용자 등록과 카드 목록을 받아오는 중
 * - `ready`: 카드 등록을 서버로 보낼 수 있다
 * - `empty`: 서버는 붙었는데 부스나 카드가 아직 없다 (어드민에서 넣어야 한다)
 * - `error`: 서버에 닿지 못했다
 */
export type CatalogState =
  | { status: 'loading' }
  | {
      status: 'ready'
      boothId: number
      serverIdOf: (mockItemId: string) => number | undefined
      unmatched: Item[]
    }
  | { status: 'empty'; reason: string }
  | { status: 'error'; reason: string }

export type CatalogValue = {
  state: CatalogState
  userId: string
  reload: () => void
}

export const CatalogContext = createContext<CatalogValue | null>(null)
