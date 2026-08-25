import { apiData, apiVoid } from '@/lib/api'

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
 */
export function registerUser(userId: string, signal?: AbortSignal): Promise<void> {
  return apiVoid('/api/users', {
    method: 'POST',
    body: JSON.stringify({ userId }),
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
