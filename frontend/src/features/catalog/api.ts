import { apiData, apiVoid } from '@/lib/api'

/**
 * 내가 지금 서버에 등록해 둔 카드 한 줄. `GET /api/have-items`, `GET /api/want-items`
 *
 * `reserved` 는 내놓을 카드에만 있다. 찾는 카드에는 예약 개념이 없다.
 */
export type RegisteredItem = {
  itemId: number
  quantity: number
  reserved?: boolean
}

/** 서버가 내려주는 부스. `GET /api/booths` */
export type ServerBooth = {
  id: number
  name: string
  description?: string
}

/** 서버가 내려주는 카드. `GET /api/booths/{boothId}/items` */
export type ServerItem = {
  id: number
  name: string
  description?: string
  imageUrl?: string
}

export function fetchBooths(signal?: AbortSignal): Promise<ServerBooth[]> {
  return apiData<ServerBooth[]>('/api/booths', { signal })
}

export function fetchBoothItems(boothId: number, signal?: AbortSignal): Promise<ServerItem[]> {
  return apiData<ServerItem[]>(`/api/booths/${boothId}/items`, { signal })
}

/**
 * 이 기기를 서버에 등록한다. 멱등이라 앱을 열 때마다 불러도 된다.
 *
 * 이걸 부르지 않으면 카드 등록이 전부 `USER_NOT_FOUND` 로 막힌다. UUID 를 만들어
 * localStorage 에 넣는 것만으로는 서버가 그 사람을 모르기 때문이다.
 *
 * 이름은 약속 화면에서 상대 줄의 라벨이 된다. 기기마다 달라야 두 사람이 붙었을 때 서로를
 * 화면에서 가릴 수 있다.
 */
export function registerUser(
  userId: string,
  username: string,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid('/api/users', {
    method: 'POST',
    body: JSON.stringify({ userId, username }),
    signal,
  })
}

/**
 * 내놓을 카드를 등록한다. 같은 카드를 다시 부르면 개수를 덮어쓴다.
 *
 * 화면이 카드마다 개수 하나를 들고 있어서, 보내는 값은 "이번에 몇 개 더" 가 아니라
 * "지금 몇 개다" 다.
 */
export function registerHaveItem(
  userId: string,
  itemId: number,
  quantity: number,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid('/api/have-items', {
    method: 'POST',
    body: JSON.stringify({ userId, itemId, quantity }),
    signal,
  })
}

/** 찾는 카드를 등록한다. 내놓을 카드와 동작이 같다. */
export function registerWantItem(
  userId: string,
  itemId: number,
  quantity: number,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid('/api/want-items', {
    method: 'POST',
    body: JSON.stringify({ userId, itemId, quantity }),
    signal,
  })
}

/**
 * 지금 서버에 등록해 둔 내놓을 카드를 읽는다.
 *
 * 등록 화면이 제출 직전에 부른다. 화면 상태는 새로고침에 사라지기 때문에, 무엇을 해제해야
 * 하는지는 서버가 들고 있는 목록과 견줘야만 알 수 있다.
 */
export function fetchMyHaveItems(userId: string, signal?: AbortSignal): Promise<RegisteredItem[]> {
  return apiData<RegisteredItem[]>(`/api/have-items?userId=${userId}`, { signal })
}

/** 지금 서버에 등록해 둔 찾는 카드를 읽는다. */
export function fetchMyWantItems(userId: string, signal?: AbortSignal): Promise<RegisteredItem[]> {
  return apiData<RegisteredItem[]>(`/api/want-items?userId=${userId}`, { signal })
}

/**
 * 내놓을 카드 등록을 해제한다. 없는 카드를 지워도 성공이다.
 *
 * 교환에 예약된 카드는 서버가 막는다(409). 그 카드를 빼려면 약속을 먼저 취소해야 한다.
 */
export function removeHaveItem(
  userId: string,
  itemId: number,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid(`/api/have-items/${itemId}?userId=${userId}`, { method: 'DELETE', signal })
}

/** 찾는 카드 등록을 해제한다. */
export function removeWantItem(
  userId: string,
  itemId: number,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid(`/api/want-items/${itemId}?userId=${userId}`, { method: 'DELETE', signal })
}
