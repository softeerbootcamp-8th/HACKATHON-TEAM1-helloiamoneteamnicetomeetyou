import { createContext } from 'react'

import type { RegisteredItem } from '@/features/catalog/api'

import type { BoothHaveItem, PokeAnswerResult, ReceivedPoke, SentPoke } from './api'

/**
 * 서버에 실제로 오간 찔러보기다.
 *
 * 스토어(`store/`)와 나누어 둔 이유는 구독 위치다. 실시간 알림은 부스를 알아야 붙을 수 있어서
 * 이쪽이 `StoreProvider` 바깥에 있고, 그래서 스토어에 직접 손대지 못한다. 둘을 잇는 일은
 * `features/poke/use-poke-sync.ts` 가 맡는다.
 */
export type PokeValue = {
  /** 내가 받은, 아직 답하지 않은 찔러보기 */
  received: ReceivedPoke[]
  /** 내가 보낸 것 전부. 대기 중인 상대의 카드를 비활성화하는 데 쓴다 */
  sent: SentPoke[]
  /** 부스 안 다른 사람들이 내놓은 카드 */
  waiting: BoothHaveItem[]
  /**
   * 상대가 내 묶음에서 고르게 될 카드들. 찔러보기 확인 화면의 "내 카드" 가 이걸 그린다.
   *
   * <b>화면이 들고 있는 선택(`state.have`)이 아니라 서버가 기준이다.</b> 수락하는 쪽에 보여줄
   * 묶음은 서버가 `PokeService.offerableItems` 로 그때그때 계산하는데, 그 규칙이 화면의 선택과
   * 두 군데서 다르다. 편집만 하고 등록을 안 마치면 화면에만 반영돼 있고, 부스를 옮기면 화면은
   * 지금 부스 카드만 남기지만 서버는 부스를 가리지 않는다. 그래서 보내기 직전 화면이 "내가 내주는
   * 카드" 라고 보여주는 것과 상대가 실제로 고르는 것이 갈렸다.
   */
  myOfferable: RegisteredItem[]
  /** 부스를 정하고 목록을 읽을 수 있는 상태인지 */
  ready: boolean
  /**
   * 목록을 한 번이라도 읽었는지.
   *
   * `ready` 는 부스를 알아냈다는 뜻일 뿐이라 첫 응답이 오기 전에도 참이다. "빈 목록"
   * 과 "아직 안 읽음" 을 갈라야 하는 쪽이 이걸 본다.
   */
  loaded: boolean
  /** 목록을 다시 읽는다. 알림을 받았을 때와 응답한 뒤에 부른다 */
  refresh: () => void
  send: (targetUserId: string, requestedItemId: number) => Promise<void>
  /**
   * 받은 찔러보기를 수락한다. 서버가 만든 교환과 주고받을 카드를 그대로 돌려준다.
   *
   * <b>이 값을 버리면 성사 화면을 그릴 수 없다.</b> 상대 묶음에서 무엇을 골랐는지와
   * 어느 교환이 생겼는지는 서버만 아는 값이라, 화면이 다시 계산해 낼 방법이 없다.
   */
  accept: (pokeId: number, chosenItemId: number) => Promise<PokeAnswerResult>
  reject: (pokeId: number) => Promise<void>
  /** 마지막으로 실패한 요청의 사유. 화면이 토스트로 띄운다 */
  error: string | null
  clearError: () => void
}

export const PokeContext = createContext<PokeValue | null>(null)
