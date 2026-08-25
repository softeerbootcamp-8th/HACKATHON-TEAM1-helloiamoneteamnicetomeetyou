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
  nickname: string
  /** 이 사람이 내놓은 카드 */
  itemId: string
  /** 이 사람이 찾는 카드들. 매칭 규칙이 실제로 이 값을 본다. */
  needsItemIds: string[]
  /** 지금 앱을 켜 두고 있는지. 목록에 "접속 중" 으로 나온다. */
  online: boolean
}

export type Zone = {
  id: string
  name: string
  location: string
  /** 약도 위 위치. 행사장 안 상대 좌표라 거리 계산에는 쓰지 않는다. */
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
  { id: 'u1', nickname: '캐스퍼', itemId: 'cas', needsItemIds: ['nv74', 'i5n'], online: true },
  { id: 'u2', nickname: '블루N', itemId: 'nv74', needsItemIds: ['sf', 'pony'], online: true },
  {
    id: 'u3',
    nickname: '아이오닉러버',
    itemId: 'i5n',
    needsItemIds: ['pony', 'nv74'],
    online: true,
  },
  { id: 'u4', nickname: 'N드라이버', itemId: 'avn', needsItemIds: ['sf'], online: false },
  { id: 'u5', nickname: '그랜저러버', itemId: 'gra', needsItemIds: ['pony', 'cas'], online: true },
]

/** 바텀시트 전체 리스트용. 레이더에 안 뜨는 사람까지 포함한다. */
export const ALL_WAITING: WaitingUser[] = [
  ...WAITING_USERS,
  { id: 'u6', nickname: '포니덕후', itemId: 'pony', needsItemIds: ['nv74', 'avn'], online: true },
  { id: 'u7', nickname: '레몬 16', itemId: 'i5n', needsItemIds: ['pony', 'sf'], online: false },
  { id: 'u8', nickname: '레몬 07', itemId: 'pony', needsItemIds: ['nv74', 'gra'], online: true },
  { id: 'u9', nickname: '싼타페러버', itemId: 'sf', needsItemIds: ['avn', 'i5n'], online: true },
  { id: 'u10', nickname: '비전러버', itemId: 'nv74', needsItemIds: ['cas'], online: false },
]

export const ZONES: Zone[] = [
  {
    id: 'z1',
    name: '중앙 포토존 앞',
    location: '행사 중앙 포토존',
    x: 52,
    y: 44,
    selectable: true,
  },
  {
    id: 'z2',
    name: '에스컬레이터',
    location: '1층 에스컬레이터 앞',
    x: 25,
    y: 68,
    selectable: false,
  },
  { id: 'z3', name: '라운지', location: '휴게 라운지', x: 82, y: 60, selectable: false },
]

/** 지정 교환장소는 1개로 한정한다. */
export const FIXED_ZONE = ZONES[0]

/** 내 식별 이름. 3인 매칭 화면의 "나 (레몬 28)" 과 같은 자리다. */
export const MY_IDENTITY = { fruit: '레몬', number: 28 }
