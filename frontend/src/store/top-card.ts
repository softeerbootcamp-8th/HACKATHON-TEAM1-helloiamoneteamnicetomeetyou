import { useMemo } from 'react'

import type { Selection } from './types'

/**
 * 카드 묶음 맨 위에 세울 카드를 고르는 수. 앱이 뜰 때 한 번만 뽑는다.
 *
 * <b>렌더 안에서 뽑지 않는다.</b> `Math.random()` 은 순수하지 않아서 React Compiler 가 막고,
 * 막지 않더라도 리렌더마다 그림이 바뀌어 카드가 깜빡인다. 그래서 앱이 뜰 때 한 번만 뽑아
 * 두고, 보유 카드 수에 곱해서 몇 번째 카드를 세울지 정한다.
 *
 * 앱을 켜 있는 동안에는 같은 카드가 서 있고 다시 켜면 달라진다. 화면에 들어올 때마다 다시
 * 뽑으려면 여기가 아니라 마운트 효과에서 뽑아야 하는데, 그러면 첫 그림이 한 번 바뀌어
 * 보인다. 깜빡이지 않는 쪽을 골랐다.
 */
const TOP_CARD_PICK = Math.random()

/**
 * 묶음 맨 위에 보이는 카드. <b>보유 카드 중에서 랜덤이다</b>
 * (시안 desc 165:3500 3번, 165:3620 1번).
 *
 * 교환 대기장의 내 카드와 찔러보기 확인 화면의 내 카드 묶음이 같은 카드를 세워야 한다.
 * 끌어다 놓은 그 카드가 다음 화면에서 다른 그림으로 바뀌면 무엇을 보낸 것인지 흐려진다.
 *
 * 보유 목록을 문자열로 눌러서 실제로 달라졌을 때만 다시 고른다. 배열을 그대로 의존성에
 * 넣으면 렌더마다 새 참조라 매번 다시 돈다. 고른 카드가 없으면 `null` 이고, 가지고 있지도
 * 않은 카드를 세우지 않는다.
 */
export function useTopHaveItemId(have: Selection[]): number | null {
  const haveKey = have.map((s) => s.itemId).join(',')
  return useMemo(() => {
    const ids = haveKey ? haveKey.split(',').map(Number) : []
    return ids.length > 0 ? ids[Math.floor(TOP_CARD_PICK * ids.length)] : null
  }, [haveKey])
}
