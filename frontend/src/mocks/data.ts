/**
 * 목업용 고정 데이터다. 백엔드 엔티티(Booth / Zone / Item / Exchange)와 이름을 맞춰 두었고,
 * API 가 붙으면 이 파일만 걷어내면 된다.
 *
 * 굿즈(goods)는 품목, 아이템(item)은 품목 안의 개별 종류다. 지금 시안은 포토카드 한 품목에
 * 차종별 아이템 7개가 들어 있는 구조라 그대로 옮겼다.
 */

export type Item = {
  id: string
  /** 카드 앞면에 크게 박히는 약칭 */
  code: string
  /** 아이템 명칭 */
  name: string
  nameKo: string
  /** 인기 순위. 낮을수록 Needs 등록이 많은 아이템이다. */
  rank: number
}

export type Goods = {
  id: string
  name: string
  items: Item[]
}

export type WaitingUser = {
  id: string
  /**
   * 서버 `users` 테이블의 UUID 다. 백엔드 `DemoUser` 의 값과 같아야 한다.
   *
   * 목업으로 고른 상대를 서버가 알아보고, 그 사람이 고른 시간이 실제로 DB 에서 읽힌다.
   * 매칭이 서버로 옮겨가면 이 목업 목록 자체가 사라진다.
   */
  userId: string
  nickname: string
  /** 이 사람이 내놓은 카드 */
  itemId: string
  /** 이 사람이 찾는 카드들. 매칭 규칙이 실제로 이 값을 본다. */
  needsItemIds: string[]
  /** 지금 앱을 켜 두고 있는지. 목록에 "접속 중" 으로 나온다. */
  online: boolean
}

/**
 * 약도 위 핀 자리다. 행사장 안 상대 좌표라 거리 계산에는 쓰지 않는다.
 *
 * 서버는 교환 장소의 이름과 위치만 알려 준다. 약도 자체가 아직 목업이라 핀을 어디에 찍을지는
 * 화면이 정하고, 서버가 준 순서대로 이 표를 얹는다. 실제 약도가 들어오면 서버가 좌표를 준다.
 */
export type ZonePin = {
  x: number
  y: number
  selectable: boolean
}

export const PHOTOCARD_ITEMS: Item[] = [
  { id: 'nv74', code: 'N74', name: 'N Vision 74', nameKo: 'N 비전 74', rank: 2 },
  { id: 'i5n', code: 'I5N', name: 'IONIQ 5 N', nameKo: '아이오닉 5 N', rank: 1 },
  { id: 'pony', code: 'P', name: 'PONY', nameKo: '포니', rank: 3 },
  { id: 'avn', code: 'AN', name: 'AVANTE N', nameKo: '아반떼 N', rank: 5 },
  { id: 'gra', code: 'G', name: 'GRANDEUR', nameKo: '그랜저', rank: 6 },
  { id: 'sf', code: 'SF', name: 'SANTA FE', nameKo: '싼타페', rank: 4 },
  { id: 'cas', code: 'C', name: 'CASPER', nameKo: '캐스퍼', rank: 7 },
]

export const GOODS: Goods[] = [{ id: 'photocard', name: '포토카드', items: PHOTOCARD_ITEMS }]

export const ALL_ITEMS = GOODS.flatMap((g) => g.items)

export function itemById(id: string): Item {
  const found = ALL_ITEMS.find((i) => i.id === id)
  if (!found) throw new Error(`알 수 없는 아이템: ${id}`)
  return found
}

/** 레이더에 뜨는 상대들. 시안이 최대 5명을 보여준다. */
export const WAITING_USERS: WaitingUser[] = [
  {
    id: 'u1',
    userId: '00000000-0000-4000-8000-000000000001',
    nickname: '캐스퍼',
    itemId: 'cas',
    needsItemIds: ['nv74', 'i5n'],
    online: true,
  },
  {
    id: 'u2',
    userId: '00000000-0000-4000-8000-000000000002',
    nickname: '블루N',
    itemId: 'nv74',
    needsItemIds: ['sf', 'pony'],
    online: true,
  },
  {
    id: 'u3',
    userId: '00000000-0000-4000-8000-000000000003',
    nickname: '아이오닉러버',
    itemId: 'i5n',
    needsItemIds: ['pony', 'nv74'],
    online: true,
  },
  {
    id: 'u4',
    userId: '00000000-0000-4000-8000-000000000004',
    nickname: 'N드라이버',
    itemId: 'avn',
    needsItemIds: ['sf'],
    online: false,
  },
  {
    id: 'u5',
    userId: '00000000-0000-4000-8000-000000000005',
    nickname: '그랜저러버',
    itemId: 'gra',
    needsItemIds: ['pony', 'cas'],
    online: true,
  },
]

/** 바텀시트 전체 리스트용. 레이더에 안 뜨는 사람까지 포함한다. */
export const ALL_WAITING: WaitingUser[] = [
  ...WAITING_USERS,
  {
    id: 'u6',
    userId: '00000000-0000-4000-8000-000000000006',
    nickname: '포니덕후',
    itemId: 'pony',
    needsItemIds: ['nv74', 'avn'],
    online: true,
  },
  {
    id: 'u7',
    userId: '00000000-0000-4000-8000-000000000007',
    nickname: '레몬 16',
    itemId: 'i5n',
    needsItemIds: ['pony', 'sf'],
    online: false,
  },
  {
    id: 'u8',
    userId: '00000000-0000-4000-8000-000000000008',
    nickname: '레몬 07',
    itemId: 'pony',
    needsItemIds: ['nv74', 'gra'],
    online: true,
  },
  {
    id: 'u9',
    userId: '00000000-0000-4000-8000-000000000009',
    nickname: '싼타페러버',
    itemId: 'sf',
    needsItemIds: ['avn', 'i5n'],
    online: true,
  },
  {
    id: 'u10',
    userId: '00000000-0000-4000-8000-000000000010',
    nickname: '비전러버',
    itemId: 'nv74',
    needsItemIds: ['cas'],
    online: false,
  },
]

/**
 * 약도 위 핀 자리. **서버가 준 교환 장소 목록의 순서대로** 얹는다.
 * 백엔드 `InitialDataSeeder` 의 순서를 바꾸면 여기도 같이 고친다.
 */
export const ZONE_PINS: ZonePin[] = [
  { x: 52, y: 44, selectable: true },
  { x: 25, y: 68, selectable: false },
  { x: 82, y: 60, selectable: false },
]

/** 서버 목록이 이 표보다 길면 화면 밖으로 나가지 않게 마지막 자리를 돌려 쓴다. */
export function zonePinAt(index: number): ZonePin {
  return ZONE_PINS[index] ?? ZONE_PINS[ZONE_PINS.length - 1]
}

/** 내 식별 이름. 3인 매칭 화면의 "나 (레몬 28)" 과 같은 자리다. */
export const MY_IDENTITY = { fruit: '레몬', number: 28 }

/**
 * 서버에 등록할 내 이름. 상대 화면에서 내 줄의 라벨이 된다.
 *
 * **기기마다 달라야 한다.** 고정값으로 두면 두 기기를 붙였을 때 상대 줄에도 내 이름과 같은
 * 글자가 뜨는데, 그러면 누가 누구인지 화면에서 가릴 수가 없다.
 *
 * 식별 화면의 "레몬 28" 과는 다른 값이다. 그쪽은 교환마다 서버가 정해 준다.
 */
export function myUsername(deviceId: string): string {
  let hash = 0
  for (const ch of deviceId) hash = (hash * 31 + ch.charCodeAt(0)) | 0
  return `손님 ${String((Math.abs(hash) % 90) + 10)}`
}
