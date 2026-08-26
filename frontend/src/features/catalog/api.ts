import { apiData, apiVoid } from '@/lib/api'

/** 서버가 내려주는 부스. `GET /api/booths` */
export type ServerBooth = {
  id: number
  name: string
  description?: string
}

/**
 * 카드 한 장. `GET /api/booths/{boothId}/items` 가 내려주는 그대로다.
 *
 * 화면 전체가 이 한 종류만 쓴다. 전에는 목업 카드와 서버 카드가 따로 있어서 카드 이름으로
 * 둘을 이어 붙였는데, 같은 이름이 둘이면 뒤엣것만 남아 등록이 엉뚱한 카드로 갔다.
 */
export type Item = {
  /** 서버가 매긴 값. 등록과 찔러보기와 매칭이 전부 이 값 하나로 돈다. */
  id: number
  name: string
  /** 한글 이름이 들어 있다. 카드 밑에 작게 붙는다. */
  description?: string
  imageUrl?: string
  /** 카드 앞면 약칭. 이미지가 없거나 안 뜰 때 이 글자가 대신 보인다. */
  code: string
}

/**
 * 내가 지금 등록해 둔 카드 한 줄. `registered` 와 `reserved` 는 찾는 카드에는 없다.
 *
 * `quantity` 와 `registered` 를 가려 써야 한다. 지금 내줄 수 있는 개수(`quantity`)는 교환에
 * 예약된 만큼이 빠져 있어서, 등록한 것이 통째로 예약되면 0 이다. 등록 화면을 되살릴 때 그 값을
 * 쓰면 매칭이 잡힌 사람의 카드가 화면에서 사라진다.
 */
export type RegisteredItem = {
  itemId: number
  /** 지금 새로 내줄 수 있는 개수. 찔러보기 묶음이 이 값을 본다. */
  quantity: number
  /** 등록해 둔 총 개수. 등록 화면을 되살릴 값은 이쪽이다. 찾는 카드에는 없다. */
  registered?: number
  reserved?: boolean
}

export function fetchBooths(signal?: AbortSignal): Promise<ServerBooth[]> {
  return apiData<ServerBooth[]>('/api/booths', { signal })
}

export function fetchBoothItems(boothId: number, signal?: AbortSignal): Promise<Item[]> {
  return apiData<Item[]>(`/api/booths/${boothId}/items`, { signal })
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

export function fetchMyHaveItems(userId: string, signal?: AbortSignal): Promise<RegisteredItem[]> {
  const query = new URLSearchParams({ userId })
  return apiData<RegisteredItem[]>(`/api/have-items?${query}`, { signal })
}

export function fetchMyWantItems(userId: string, signal?: AbortSignal): Promise<RegisteredItem[]> {
  const query = new URLSearchParams({ userId })
  return apiData<RegisteredItem[]>(`/api/want-items?${query}`, { signal })
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
  const query = new URLSearchParams({ userId })
  return apiVoid(`/api/have-items/${itemId}?${query}`, { method: 'DELETE', signal })
}

/** 찾는 카드 등록을 해제한다. 내놓을 카드와 동작이 같다. */
export function removeWantItem(
  userId: string,
  itemId: number,
  signal?: AbortSignal,
): Promise<void> {
  const query = new URLSearchParams({ userId })
  return apiVoid(`/api/want-items/${itemId}?${query}`, { method: 'DELETE', signal })
}
