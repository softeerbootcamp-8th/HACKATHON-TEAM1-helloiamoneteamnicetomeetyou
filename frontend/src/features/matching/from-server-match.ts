import type { WaitingUser } from '@/mocks/data'
import type { ExchangePair } from '@/store/matching'
import type { ActiveMatch } from '@/store/types'

/**
 * `MATCH_SUGGESTED` SSE 이벤트로 오는 데이터. 백엔드
 * `MatchSuggestedResponseDto` 와 같은 모양이다. 한쪽만 고치면 조용히 어긋나므로 같이 고친다.
 */
export type ServerMatchedUser = { id: string; username: string | null }
export type ServerMatchedItem = {
  id: number
  name: string
  imageUrl: string | null
  quantity: number
}
export type ServerMatchSuggested = {
  exchangeId: number
  type: 'ONE_TO_ONE' | 'MULTI_WAY'
  giveItems: ServerMatchedItem[]
  giveTo: ServerMatchedUser
  receiveItems: ServerMatchedItem[]
  receiveFrom: ServerMatchedUser
  middleItems: ServerMatchedItem[]
}

/**
 * 서버 카드를 목업 카드 id 로 바꾸면서 수량만큼 펼친다.
 *
 * `{id:1, quantity:2}` 하나가 카드 두 장이다. 펼치지 않고 항목 수로만 세면 종류가 다른데
 * 수량이 몰린 쪽과 종류는 많은데 한 장씩인 쪽의 배열 길이가 달라져서, 인덱스로 카드를
 * 맞춰 그릴 때 한쪽이 남아 화면에서 빠진다. 백엔드가 보장하는 것은 "양쪽 총 장수가 같다"
 * 뿐이라(어느 카드가 어느 카드와 정확히 짝인지는 정해져 있지 않다), 펼친 배열의 길이를
 * 맞추는 것으로 그 보장을 그대로 옮긴다.
 *
 * 이름이 안 맞아 매핑을 못 찾은 카드는 조용히 빠진다. 어드민 카드 이름과 목업 이름이
 * 어긋난 경우인데, 이 카드 하나가 없다고 매칭 전체를 화면에서 지우는 것보다 낫다.
 */
function expandToMockIds(
  items: ServerMatchedItem[],
  mockIdOf: (serverItemId: number) => string | undefined,
): string[] {
  const result: string[] = []
  for (const item of items) {
    const mockId = mockIdOf(item.id)
    if (mockId === undefined) continue
    for (let i = 0; i < item.quantity; i += 1) result.push(mockId)
  }
  return result
}

/**
 * 실제 매칭 상대는 닉네임 말고 화면이 쓰는 정보가 없다. `WaitingUser` 의 나머지 필드는
 * 레이더·찔러보기 같은 목업 흐름 전용이라 여기서는 빈 값으로 채운다.
 */
function toWaitingUser(user: ServerMatchedUser): WaitingUser {
  return {
    id: user.id,
    // 서버 매칭 상대는 목업 id 가 따로 없다. 둘 다 서버가 준 UUID 다.
    userId: user.id,
    nickname: user.username ?? '상대',
    itemId: '',
    needsItemIds: [],
    online: true,
  }
}

/**
 * SSE 로 받은 매칭 결과를 화면이 쓰는 `ActiveMatch` 로 바꾼다.
 *
 * 카드 매핑이 하나도 안 되면(둘 다 이름이 안 맞음) `null` 을 돌려준다. 빈 카드로 화면을
 * 그리느니 이 알림을 조용히 무시하는 편이 낫다.
 */
export function fromServerMatch(
  dto: ServerMatchSuggested,
  mockIdOf: (serverItemId: number) => string | undefined,
): ActiveMatch | null {
  const giveIds = expandToMockIds(dto.giveItems, mockIdOf)
  const receiveIds = expandToMockIds(dto.receiveItems, mockIdOf)
  if (giveIds.length === 0 || receiveIds.length === 0) return null

  if (dto.type === 'ONE_TO_ONE') {
    const count = Math.min(giveIds.length, receiveIds.length)
    const pairs: ExchangePair[] = Array.from({ length: count }, (_, i) => ({
      giveItemId: giveIds[i],
      receiveItemId: receiveIds[i],
    }))
    return {
      kind: 'ONE_TO_ONE',
      partner: toWaitingUser(dto.giveTo),
      pairs,
      giveItemId: pairs[0].giveItemId,
      receiveItemId: pairs[0].receiveItemId,
      origin: 'auto',
      exchangeId: dto.exchangeId,
    }
  }

  const middleId = expandToMockIds(dto.middleItems, mockIdOf)[0]
  if (!middleId) return null

  return {
    kind: 'THREE_WAY',
    // 고리는 나 → giveTo → receiveFrom → 나 로 돈다.
    giver: toWaitingUser(dto.receiveFrom),
    receiver: toWaitingUser(dto.giveTo),
    giveItemId: giveIds[0],
    receiveItemId: receiveIds[0],
    middleItemId: middleId,
    origin: 'auto',
    exchangeId: dto.exchangeId,
  }
}
