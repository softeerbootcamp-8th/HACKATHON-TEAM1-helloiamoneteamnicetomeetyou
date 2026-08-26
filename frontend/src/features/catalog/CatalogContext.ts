import { createContext } from 'react'

import type { Item, ServerBooth } from './api'

/**
 * 서버 연동 준비 상태.
 *
 * - `loading`: 사용자 등록과 카드 목록을 받아오는 중
 * - `ready`: 카드 목록을 받았다. 등록 화면이 이걸 그린다
 * - `empty`: 서버는 붙었는데 부스나 카드가 아직 없다 (어드민에서 넣어야 한다)
 * - `error`: 서버에 닿지 못했다
 *
 * **`ready` 가 아니면 화면은 카드를 그리지 않는다.** 전에는 목업 카드로 떨어졌는데, 그러면
 * 고를 수는 있지만 서버에 없는 카드라 등록도 매칭도 되지 않는 상태로 흐름을 계속 타게 된다.
 */
export type CatalogState =
  | { status: 'loading' }
  | {
      status: 'ready'
      boothId: number
      /** 이 부스가 내놓은 카드 전부. 등록 화면이 이 순서 그대로 그린다. */
      items: Item[]
      /** 서버 카드 id 로 카드를 찾는다. 목록에 없는 id 면 `undefined` 다. */
      itemById: (itemId: number) => Item | undefined
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
