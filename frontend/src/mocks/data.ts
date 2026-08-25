/**
 * 목업용 고정 데이터다. 백엔드 엔티티(Booth / Zone / Item / Exchange)와 이름을 맞춰 두었고,
 * API 가 붙으면 이 파일만 걷어내면 된다.
 *
 * 굿즈(goods)는 품목, 아이템(item)은 품목 안의 개별 종류다. 지금 시안은 포토카드 한 품목에
 * 차종별 아이템 9개가 들어 있는 구조라 그대로 옮겼다.
 *
 * 이미지는 시연에서 실제로 쓸 Supabase 스토리지의 파일을 그대로 가리킨다. 이름이 서버에
 * 등록된 카드 이름과 같아야 `features/catalog/match-by-name.ts` 가 짝을 찾는다.
 */

const IMAGE_BASE = 'https://sdumqvkniemiowanvsef.supabase.co/storage/v1/object/public/items'

export type Item = {
  id: string
  /** 카드 앞면에 크게 박히는 약칭. 이미지가 안 뜰 때 이 글자가 대신 보인다. */
  code: string
  /** 아이템 명칭 */
  name: string
  nameKo: string
  /** 카드 앞면 이미지 */
  image: string
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
  /** 이 사람이 내놓은 카드. 레이더와 전체리스트에는 이 카드가 대표로 뜬다. */
  itemId: string
  /** 대표 카드 말고 더 들고 있는 카드. 한 번에 여러 장을 바꾸는 경우를 만든다. */
  alsoHasItemIds?: string[]
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
  {
    id: 'i5n',
    code: 'I5N',
    name: 'IONIQ 5 N',
    nameKo: '아이오닉 5 N',
    image: `${IMAGE_BASE}/IONIQ5_N.png`,
    rank: 1,
  },
  {
    id: 'avn',
    code: 'AN',
    name: 'AVANTE N',
    nameKo: '아반떼 N',
    image: `${IMAGE_BASE}/AVANTE_N.png`,
    rank: 2,
  },
  {
    id: 'vel',
    code: 'VN',
    name: 'VELOSTER N',
    nameKo: '벨로스터 N',
    image: `${IMAGE_BASE}/VELOSTER_N.png`,
    rank: 3,
  },
  {
    id: 'kona',
    code: 'KN',
    name: 'KONA N',
    nameKo: '코나 N',
    image: `${IMAGE_BASE}/KONA_N.png`,
    rank: 4,
  },
  {
    id: 'i30n',
    code: 'I30',
    name: 'i30 N',
    nameKo: 'i30 N',
    image: `${IMAGE_BASE}/i30_N.png`,
    rank: 5,
  },
  {
    id: 'i30f',
    code: 'I30F',
    name: 'i30 Fastback',
    nameKo: 'i30 패스트백',
    image: `${IMAGE_BASE}/i30_Fastback.png`,
    rank: 6,
  },
  {
    id: 'i20n',
    code: 'I20',
    name: 'i20 N',
    nameKo: 'i20 N',
    image: `${IMAGE_BASE}/i20_N.png`,
    rank: 7,
  },
  {
    id: 'avnf',
    code: 'ANF',
    name: 'AVANTE N Facelift',
    nameKo: '아반떼 N 페이스리프트',
    // 파일 이름에 공백이 들어 있어 그대로 두면 주소가 끊긴다.
    image: `${IMAGE_BASE}/AVANTE_N%20Facelift.png`,
    rank: 8,
  },
  {
    id: 'i20r',
    code: 'I20R',
    name: 'i20 N Rally1',
    nameKo: 'i20 N 랠리1',
    image: `${IMAGE_BASE}/i20_N_Rally1.png`,
    rank: 9,
  },
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
    nickname: '코나러버',
    itemId: 'kona',
    alsoHasItemIds: ['vel'],
    needsItemIds: ['avn', 'i5n'],
    online: true,
  },
  { id: 'u2', nickname: '블루N', itemId: 'vel', needsItemIds: ['i30n', 'i20n'], online: true },
  {
    id: 'u3',
    nickname: '아이오닉러버',
    itemId: 'i5n',
    alsoHasItemIds: ['i30f'],
    needsItemIds: ['i20n', 'avn'],
    online: true,
  },
  { id: 'u4', nickname: 'N드라이버', itemId: 'avn', needsItemIds: ['i30n'], online: false },
  { id: 'u5', nickname: '랠리팬', itemId: 'i20r', needsItemIds: ['i20n', 'kona'], online: true },
]

/** 바텀시트 전체 리스트용. 레이더에 안 뜨는 사람까지 포함한다. */
export const ALL_WAITING: WaitingUser[] = [
  ...WAITING_USERS,
  {
    id: 'u6',
    nickname: '패스트백덕후',
    itemId: 'i30f',
    needsItemIds: ['avn', 'vel'],
    online: true,
  },
  { id: 'u7', nickname: '레몬 16', itemId: 'i5n', needsItemIds: ['i20n', 'i30n'], online: false },
  { id: 'u8', nickname: '레몬 07', itemId: 'i20n', needsItemIds: ['avn', 'i20r'], online: true },
  { id: 'u9', nickname: '아반떼러버', itemId: 'avnf', needsItemIds: ['kona', 'i5n'], online: true },
  { id: 'u10', nickname: '삼공러버', itemId: 'i30n', needsItemIds: ['i30f'], online: false },
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
