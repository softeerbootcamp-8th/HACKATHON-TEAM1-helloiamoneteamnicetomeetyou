import { createContext } from 'react'

import type { Item } from '@/mocks/data'

import type { ServerBooth } from './api'

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
      /** 서버 카드 id → 목업 카드 id. SSE 로 오는 매칭 알림을 화면에 그릴 때 쓴다. */
      mockIdOf: (serverItemId: number) => string | undefined
      /** 서버 카드 id → 목업 카드. 서버에서 받은 것을 화면에 그릴 때 쓴다. */
      mockItemOf: (serverItemId: number) => Item | undefined
      unmatched: Item[]
    }
  | { status: 'empty'; reason: string }
  | { status: 'error'; reason: string }

export type CatalogValue = {
  state: CatalogState
  userId: string
  reload: () => void
  /** 서버에 열려 있는 부스 전부. 랜딩의 부스 고르기가 쓴다. */
  booths: ServerBooth[]
  /**
   * 지금 붙어 있는 부스. 목록을 받기 전이면 `null` 이다.
   *
   * `state.boothId` 와 따로 두는 이유는, 카드 목록을 받는 동안이나 그 부스에 카드가 하나도
   * 없을 때(`empty`)에도 **부스 이름은 화면에 떠 있어야** 하기 때문이다. `ready` 안에만
   * 두면 정작 "이 부스엔 카드가 없다" 를 말해야 할 때 어느 부스인지 못 보여준다.
   */
  booth: ServerBooth | null
  /** 부스를 바꾼다. 고른 부스는 다음 방문까지 남는다. */
  selectBooth: (boothId: number) => void
}

export const CatalogContext = createContext<CatalogValue | null>(null)
