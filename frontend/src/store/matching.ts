import { ALL_WAITING, type WaitingUser } from '@/mocks/data'
import { itemById } from '@/mocks/data'

/**
 * 매칭 규칙을 여기 모아 둔다. 목업이지만 화면에 보이는 결과가 실제로 이 함수에서
 * 나와야 흐름이 말이 된다. 데이터만 가짜고 판정은 진짜다.
 *
 * 기획서의 우선순위를 그대로 옮겼다.
 * 1. 1:1 교환이 가능한 상대를 먼저 찾는다
 * 2. 없으면 삼자 교환을 찾는다 (한 사람당 한 아이템만)
 * 3. 둘 다 없으면 실패로 두고 계속 기다린다
 *
 * 후보가 여럿이면 인기 많은 아이템(rank 가 작은 쪽)을 가진 상대를 고른다.
 */

export type OneToOneMatch = {
  kind: 'ONE_TO_ONE'
  partner: WaitingUser
  /** 내가 주는 카드 */
  giveItemId: string
  /** 내가 받는 카드 */
  receiveItemId: string
}

export type ThreeWayMatch = {
  kind: 'THREE_WAY'
  /** 나에게 카드를 주는 사람 */
  giver: WaitingUser
  /** 내가 카드를 주는 사람 */
  receiver: WaitingUser
  giveItemId: string
  receiveItemId: string
  /** giver 가 receiver 에게 넘기는 카드 */
  middleItemId: string
}

export type MatchResult = OneToOneMatch | ThreeWayMatch

function byPopularity(a: WaitingUser, b: WaitingUser) {
  return itemById(a.itemId).rank - itemById(b.itemId).rank
}

export function findMatch(haveIds: string[], needIds: string[]): MatchResult | null {
  if (haveIds.length === 0 || needIds.length === 0) return null

  const have = new Set(haveIds)
  const need = new Set(needIds)

  // 1. 서로 원하는 것이 정확히 맞는 상대
  const direct = ALL_WAITING.filter((u) => need.has(u.itemId) && have.has(u.needsItemId)).sort(
    byPopularity,
  )

  if (direct.length > 0) {
    const partner = direct[0]
    return {
      kind: 'ONE_TO_ONE',
      partner,
      giveItemId: partner.needsItemId,
      receiveItemId: partner.itemId,
    }
  }

  // 2. 삼자 교환. 내가 A 에게 받고, A 가 원하는 것을 B 가 가지고 있고,
  //    B 는 내가 가진 것을 원하는 고리를 찾는다.
  const givers = ALL_WAITING.filter((u) => need.has(u.itemId)).sort(byPopularity)
  for (const giver of givers) {
    const receivers = ALL_WAITING.filter(
      (u) => u.id !== giver.id && u.itemId === giver.needsItemId && have.has(u.needsItemId),
    ).sort(byPopularity)
    if (receivers.length > 0) {
      const receiver = receivers[0]
      return {
        kind: 'THREE_WAY',
        giver,
        receiver,
        giveItemId: receiver.needsItemId,
        receiveItemId: giver.itemId,
        middleItemId: receiver.itemId,
      }
    }
  }

  // 3. 경우의 수가 없다. 실패로 두고 계속 기다린다.
  return null
}

/**
 * 레이더에 띄울 상대를 고른다. 내가 원하는 굿즈를 가졌고 아직 약속이 없는 사람만,
 * 최대 5명이다. 원하는 것이 없으면 인기순으로 채워서 화면이 비지 않게 한다.
 */
export function radarUsers(needIds: string[]): WaitingUser[] {
  const need = new Set(needIds)
  const wanted = ALL_WAITING.filter((u) => need.has(u.itemId))
  const rest = ALL_WAITING.filter((u) => !need.has(u.itemId)).sort(byPopularity)
  const seen = new Set<string>()
  return [...wanted, ...rest]
    .filter((u) => {
      // 같은 아이템을 든 사람이 둘 이상이면 레이더에는 한 명만 세운다.
      if (seen.has(u.itemId)) return false
      seen.add(u.itemId)
      return true
    })
    .slice(0, 5)
}

/**
 * 바텀시트 리스트 정렬. 내 Needs 를 위로 올리고, 그 안에서는 인기 많은 순이다.
 */
export function sortedWaitingList(needIds: string[]): WaitingUser[] {
  const need = new Set(needIds)
  return [...ALL_WAITING].sort((a, b) => {
    const aWanted = need.has(a.itemId) ? 0 : 1
    const bWanted = need.has(b.itemId) ? 0 : 1
    if (aWanted !== bWanted) return aWanted - bWanted
    return byPopularity(a, b)
  })
}
