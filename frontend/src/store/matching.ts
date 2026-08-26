import { ALL_WAITING, type WaitingUser } from '@/mocks/data'
import { itemById } from '@/mocks/data'

/** 주고받을 카드 한 쌍 */
export type ExchangePair = {
  /** 내가 주는 카드 */
  giveItemId: string
  /** 내가 받는 카드 */
  receiveItemId: string
}

export type OneToOneMatch = {
  kind: 'ONE_TO_ONE'
  partner: WaitingUser
  /** 주고받을 카드 쌍 전부. 한 번에 여러 장을 바꾸면 둘 이상이 된다. */
  pairs: ExchangePair[]
  /** 첫 쌍. 한 줄로 줄여 보여주는 자리와 교환 정리에서 쓴다. */
  giveItemId: string
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

/** 이 사람이 들고 있는 카드 전부. 대표 카드 말고도 더 가지고 있을 수 있다. */
export function heldBy(user: WaitingUser): string[] {
  return [user.itemId, ...(user.alsoHasItemIds ?? [])]
}

/** 이 사람이 내 카드 중 하나라도 원하는지. 원하면 그중 가장 인기 있는 것을 돌려준다. */
export function wantedFromMe(user: WaitingUser, haveIds: string[]): string[] {
  return haveIds
    .filter((id) => user.needsItemIds.includes(id))
    .sort((a, b) => itemById(a).rank - itemById(b).rank)
}

/**
 * 레이더 후보. 내가 원하는 굿즈를 가진 사람을 먼저 세우고, 모자라면 인기순으로 채운다.
 * 같은 아이템을 든 사람이 둘 이상이면 레이더에는 한 명만 세운다.
 */
function radarPool(needIds: string[]): WaitingUser[] {
  const need = new Set(needIds)
  const wanted = ALL_WAITING.filter((u) => need.has(u.itemId))
  const rest = ALL_WAITING.filter((u) => !need.has(u.itemId)).sort(byPopularity)
  const seen = new Set<string>()
  return [...wanted, ...rest].filter((u) => {
    if (seen.has(u.itemId)) return false
    seen.add(u.itemId)
    return true
  })
}

export const RADAR_SIZE = 5

/**
 * 레이더에 띄울 상대를 고른다. 최대 5명이다.
 *
 * `page` 는 "다른 카드 보기" 를 누른 횟수다. 화면에 떠 있던 카드보다 뒷순위를 보여주고,
 * 뒷순위가 모자라면 앞에서부터 다시 채운다. 답변을 기다리는 중인 상대(`pinnedIds`)는
 * 새로고침해도 자리를 지킨다. 다시 신청할 수 없다는 것이 화면에 계속 남아 있어야 한다.
 */
export function radarUsers(needIds: string[], page = 0, pinnedIds: string[] = []): WaitingUser[] {
  const pool = radarPool(needIds)
  if (pool.length === 0) return []

  const pinned = pool.filter((u) => pinnedIds.includes(u.id))
  const rest = pool.filter((u) => !pinnedIds.includes(u.id))
  const room = Math.max(RADAR_SIZE - pinned.length, 0)

  const picked: WaitingUser[] = []
  const seen = new Set(pinned.map((u) => u.id))
  for (let i = 0; picked.length < room && i < rest.length; i += 1) {
    const user = rest[(page * room + i) % rest.length]
    if (seen.has(user.id)) continue
    seen.add(user.id)
    picked.push(user)
  }

  return [...pinned, ...picked]
}

/**
 * 전체리스트에 올릴 상대. 내 Needs 와 맞는 아이템만 보여준다.
 * 아직 Needs 를 하나도 안 골랐으면 화면이 비어 버리므로 전체를 인기순으로 보여준다.
 */
export function sortedWaitingList(needIds: string[]): WaitingUser[] {
  const need = new Set(needIds)
  const matched = ALL_WAITING.filter((u) => heldBy(u).some((id) => need.has(id)))
  const source = matched.length > 0 ? matched : ALL_WAITING
  return [...source].sort(byPopularity)
}

/** 전체리스트 한 줄의 오른쪽에 붙는 상태. 시안이 세 가지로 나눠 둔다. */
export type WaitingStatus = '매칭됨' | '교환 가능' | '그래도 찔러보기'

export function waitingStatus(
  user: WaitingUser,
  haveIds: string[],
  matchedUserIds: string[],
): WaitingStatus {
  if (matchedUserIds.includes(user.id)) return '매칭됨'
  return wantedFromMe(user, haveIds).length > 0 ? '교환 가능' : '그래도 찔러보기'
}
