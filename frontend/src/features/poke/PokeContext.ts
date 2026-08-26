import { createContext } from 'react'

import type { BoothHaveItem, ReceivedPoke, SentPoke } from './api'

/**
 * 서버에 실제로 오간 찔러보기다. 목업 흐름(`store/`)과 나란히 돈다.
 *
 * 둘을 합치지 않은 이유: 목업은 카드 id 가 문자열이고 상대가 `ALL_WAITING` 의 가짜 사용자다.
 * 서버는 숫자 카드 id 와 UUID 를 쓴다. 한 상태에 섞으면 어느 쪽 값인지 따라다니며 갈라야
 * 하는데, 매칭 알고리즘(#20)이 들어오면 목업 쪽이 통째로 사라진다. 그때 이 파일만 남는다.
 */
export type PokeValue = {
  /** 내가 받은, 아직 답하지 않은 찔러보기 */
  received: ReceivedPoke[]
  /** 내가 보낸 것 전부. 대기 중인 상대의 카드를 비활성화하는 데 쓴다 */
  sent: SentPoke[]
  /** 부스 안 다른 사람들이 내놓은 카드 */
  waiting: BoothHaveItem[]
  /** 서버 연동이 준비됐는지. 아니면 화면이 목업으로 돈다 */
  ready: boolean
  /** 목록을 다시 읽는다. 알림을 받았을 때와 응답한 뒤에 부른다 */
  refresh: () => void
  send: (targetUserId: string, requestedItemId: number) => Promise<void>
  accept: (pokeId: number, chosenItemId: number) => Promise<void>
  reject: (pokeId: number) => Promise<void>
  /** 마지막으로 실패한 요청의 사유. 화면이 토스트로 띄운다 */
  error: string | null
  clearError: () => void
}

export const PokeContext = createContext<PokeValue | null>(null)
