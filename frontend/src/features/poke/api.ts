import type { Item } from '@/features/catalog/api'
import { apiData } from '@/lib/api'

/** 서버가 내려주는 목록 껍데기. 형식은 `contracts.md` 가 기준이다. */
export type PageResponse<T> = {
  content: T[]
  /** 커서 방식으로 바꿀 때를 위해 자리만 잡아 둔 값이라 지금은 항상 null 이다. */
  nextCursor: string | null
  hasNext: boolean
  /** 요청한 크기가 아니라 실제로 담긴 개수다. */
  size: number
}

/**
 * 서버 카드. 카드 등록 화면이 받는 것과 같은 형식이라 그대로 그릴 수 있다.
 *
 * 목록 응답이 카드를 통째로 싣고 오기 때문에 화면이 따로 카드를 찾을 일이 없다. 전에는
 * 여기 담긴 것이 id 와 이름뿐이라 그림을 목업에서 이름으로 찾아 왔고, 못 찾은 카드는
 * 화면에서 조용히 빠졌다.
 */
export type ServerItemRef = Item

/**
 * 부스 안 다른 사람이 내놓은 카드 한 줄. `GET /api/booths/{boothId}/have-items`
 *
 * 목록에 무엇이 담기는지는 내가 찾는 카드를 등록했는지에 따라 서버가 가른다.
 * 등록했으면 그와 맞는 줄만, 아직이면 내가 가진 카드를 뺀 전부다 (시안 desc 204:4928).
 *
 * 상태 배지는 세 가지를 위에서부터 본다 (시안 desc 204:4948).
 * - 매칭됨: `matched`. 이 사람과 진행 중인 교환이 있다
 * - 교환 가능: 줄 수 있는 카드가 있다
 * - 그래도 찔러보기: 줄 수 있는 카드가 없다
 */
export type BoothHaveItem = {
  haveItemId: number
  ownerId: string
  ownerName?: string
  item: ServerItemRef
  quantity: number
  /** 내 희망 카드인가. 희망 카드를 하나도 등록하지 않았으면 전부 false 다 */
  wanted: boolean
  /** 이 사람과 지금 진행 중인 교환이 있는가 */
  matched: boolean
  givableItemNames: string[]
  ownerWantedItemNames: string[]
}

/** 내가 받은 찔러보기. `offeredItems` 중 한 장을 골라 수락한다. */
export type ReceivedPoke = {
  pokeId: number
  fromUserId: string
  fromUserName?: string
  requestedItem: ServerItemRef
  offeredItems: ServerItemRef[]
  createdAt: string
}

export type PokeStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

/**
 * 내가 보낸 찔러보기.
 *
 * `chosenItem` 은 상대가 내 묶음에서 고른 카드다. **서버만 아는 값이라 반드시 여기서 받아
 * 써야 한다.** 화면이 짐작하면, 찔러보기는 정의상 "상대 희망 ∩ 내 보유" 가 비어 있어서
 * 늘 엉뚱한 카드를 집는다.
 */
export type SentPoke = {
  pokeId: number
  targetUserId: string
  targetUserName?: string
  status: PokeStatus
  requestedItem: ServerItemRef
  chosenItem?: ServerItemRef
  exchangeId?: number
  createdAt: string
  respondedAt?: string
}

export type PokeAnswerResult = {
  pokeId: number
  status: PokeStatus
  exchangeId?: number
  /** 답한 사람이 내주는 카드 */
  giveItemId?: number
  /** 답한 사람이 받는 카드 */
  receiveItemId?: number
}

/** 실시간 알림에 실려 오는 내용. 일부러 작아서, 받으면 목록을 다시 읽는다. */
export type PokeEvent = {
  pokeId: number
  requestedItemId: number
  chosenItemId?: number
  exchangeId?: number
}

export function fetchBoothHaveItems(
  boothId: number,
  userId: string,
  signal?: AbortSignal,
): Promise<PageResponse<BoothHaveItem>> {
  const query = new URLSearchParams({ userId, page: '0', size: '50' })
  return apiData<PageResponse<BoothHaveItem>>(`/api/booths/${boothId}/have-items?${query}`, {
    signal,
  })
}

export function sendPoke(
  userId: string,
  targetUserId: string,
  requestedItemId: number,
  signal?: AbortSignal,
): Promise<{ pokeId: number }> {
  return apiData<{ pokeId: number }>('/api/pokes', {
    method: 'POST',
    body: JSON.stringify({ userId, targetUserId, requestedItemId }),
    signal,
  })
}

export function fetchReceivedPokes(
  userId: string,
  signal?: AbortSignal,
): Promise<PageResponse<ReceivedPoke>> {
  const query = new URLSearchParams({ userId, page: '0', size: '20' })
  return apiData<PageResponse<ReceivedPoke>>(`/api/pokes/received?${query}`, { signal })
}

export function fetchSentPokes(
  userId: string,
  signal?: AbortSignal,
): Promise<PageResponse<SentPoke>> {
  const query = new URLSearchParams({ userId, page: '0', size: '20' })
  return apiData<PageResponse<SentPoke>>(`/api/pokes/sent?${query}`, { signal })
}

/**
 * 받은 찔러보기에 답한다. 수락과 거절이 같은 엔드포인트다.
 *
 * 팀 URL 규약이 "kebab-case 에 복수 명사" 라서 `/pokes/{id}/accept` 같은 동사 경로를 두지
 * 않는다. 수락이면 `chosenItemId` 가 반드시 있어야 한다.
 */
export function answerPoke(
  pokeId: number,
  userId: string,
  status: Extract<PokeStatus, 'ACCEPTED' | 'REJECTED'>,
  chosenItemId?: number,
  signal?: AbortSignal,
): Promise<PokeAnswerResult> {
  return apiData<PokeAnswerResult>(`/api/pokes/${pokeId}`, {
    method: 'PATCH',
    body: JSON.stringify({ userId, status, chosenItemId }),
    signal,
  })
}
