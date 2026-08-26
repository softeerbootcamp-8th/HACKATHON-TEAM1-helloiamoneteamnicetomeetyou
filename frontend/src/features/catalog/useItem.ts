import type { Item } from './api'
import { useCatalog } from './useCatalog'

/**
 * 목록에 없는 카드를 그릴 때 쓰는 자리표시자.
 *
 * 부스를 옮겼거나 어드민에서 지운 카드가 지난 약속에 남아 있을 수 있다. 그 한 장 때문에
 * 화면 전체를 비우지 않고, 자리는 지키되 모르는 카드라고 적는다.
 */
export function unknownItem(itemId: number): Item {
  return { id: itemId, name: '알 수 없는 카드', code: '?' }
}

/**
 * 카드 id 로 카드를 찾는다. 목록을 아직 못 받았거나 이 부스에 없는 카드면 `undefined` 다.
 *
 * 전에는 목업 배열을 뒤지는 전역 함수(`itemById`)였는데, 찾지 못하면 예외를 던져서 카드
 * 하나 때문에 화면이 통째로 죽었다. 지금은 서버 목록이 기준이라 없을 수 있는 것이 정상이고,
 * 부르는 쪽이 무엇을 그릴지 정한다.
 */
export function useItem(itemId: number | null | undefined): Item | undefined {
  const { state } = useCatalog()
  if (itemId === null || itemId === undefined) return undefined
  return state.status === 'ready' ? state.itemById(itemId) : undefined
}
