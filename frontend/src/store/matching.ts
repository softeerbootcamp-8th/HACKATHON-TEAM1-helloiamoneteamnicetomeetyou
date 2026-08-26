/**
 * 매칭 상대. 화면이 상대에게서 실제로 쓰는 것은 식별자와 표시 이름뿐이다.
 *
 * 전에는 목업 대기자(`WaitingUser`)를 그대로 담았는데, 서버에서 온 상대는 그 형태에 채울
 * 값이 없어서 빈 문자열과 빈 배열로 메워 넣고 있었다. 실제로 쓰는 두 개만 남긴다.
 */
export type MatchPartner = {
  /** 서버가 준 사용자 UUID */
  id: string
  /** 표시 이름. 서버가 안 보낸 사용자는 "상대" 로 들어간다. */
  nickname: string
}

/** 주고받을 카드 한 쌍. 서버 카드 id 다. */
export type ExchangePair = {
  /** 내가 주는 카드 */
  giveItemId: number
  /** 내가 받는 카드 */
  receiveItemId: number
}

export type OneToOneMatch = {
  kind: 'ONE_TO_ONE'
  partner: MatchPartner
  /** 주고받을 카드 쌍 전부. 한 번에 여러 장을 바꾸면 둘 이상이 된다. */
  pairs: ExchangePair[]
  /** 첫 쌍. 한 줄로 줄여 보여주는 자리와 교환 정리에서 쓴다. */
  giveItemId: number
  receiveItemId: number
}

export type ThreeWayMatch = {
  kind: 'THREE_WAY'
  /** 나에게 카드를 주는 사람 */
  giver: MatchPartner
  /** 내가 카드를 주는 사람 */
  receiver: MatchPartner
  giveItemId: number
  receiveItemId: number
  /** giver 가 receiver 에게 넘기는 카드 */
  middleItemId: number
}

export type MatchResult = OneToOneMatch | ThreeWayMatch

/** 전체리스트 한 줄의 오른쪽에 붙는 상태. 시안이 세 가지로 나눠 둔다. */
export type WaitingStatus = '매칭됨' | '교환 가능' | '그래도 찔러보기'

/**
 * 이 교환에서 실제로 주고받는 카드 쌍 전부. 삼자 교환은 언제나 한 쌍이다.
 *
 * `giveItemId`/`receiveItemId` 는 한 줄로 줄여 보여주는 자리에 쓰는 첫 쌍이라,
 * 무엇을 주고받았는지를 빠짐없이 세야 하는 곳은 이쪽을 쓴다.
 */
export function pairsOf(match: MatchResult): ExchangePair[] {
  if (match.kind === 'ONE_TO_ONE') return match.pairs
  return [{ giveItemId: match.giveItemId, receiveItemId: match.receiveItemId }]
}
