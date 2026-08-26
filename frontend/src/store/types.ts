import type { Zone } from '@/lib/exchange'

import type { MatchResult } from './matching'

export type Selection = { itemId: string; qty: number }

export type NotificationKind =
  | 'match'
  | 'poke-received'
  | 'poke-accepted'
  | 'poke-rejected'
  | 'time-request'
  | 'time-matched'
  | 'match-rejected'

export type AppNotification = {
  id: string
  kind: NotificationKind
  title: string
  body: string
}

export type OutgoingPoke = {
  targetUserId: string
  /** 상대에게 요청한 카드 */
  wantItemId: string
  status: 'pending' | 'accepted' | 'rejected'
}

export type IncomingPoke = {
  fromUserId: string
  /** 상대가 원하는 내 카드 */
  wantItemId: string
  /** 상대가 내놓은 카드 묶음. 이 중 한 장을 고른다. */
  offeredItemIds: string[]
}

export type AppointmentStage =
  'time-waiting' | 'time-conflict' | 'confirmed' | 'arrived' | 'completed'

/** 약속 화면의 상대 한 명. 서버가 내려주는 참가자에서 나를 뺀 것이다. */
export type AppointmentPartner = {
  userId: string
  /** 이름을 안 보낸 사용자는 "상대" 로 들어간다. */
  name: string
  slots: number[]
  arrived: boolean
}

/**
 * 서버가 들고 있는 교환을 화면이 쓰기 좋게 옮겨 담은 것이다. 원본은 `Exchange` 고,
 * 실시간 알림을 받을 때마다 다시 읽어 이걸 갈아끼운다.
 *
 * 약속을 동시에 두 개 이상 잡을 수 있어서 배열로 들고, `exchangeId` 로 가른다. 화면 안에서만
 * 쓰는 별도의 id 를 두지 않는 것은 서버가 이미 부여한 값이 있는데 두 벌을 맞춰 줄 이유가 없어서다.
 */
export type Appointment = {
  exchangeId: number
  /**
   * 이 약속이 성사시킨 교환. 약속마다 상대와 카드가 다르기 때문에 약속이 들고 있는다.
   *
   * **없을 수 있다.** 무엇을 주고받는지는 아직 화면 목업이 정하는 값이라 서버가 모른다.
   * 매칭을 거치지 않고 약속으로 들어온 경우(새로고침, 상대가 만든 약속)에는 비어 있고,
   * 그때 화면은 카드 그림만 빼고 나머지를 보여준다.
   */
  match: ActiveMatch | null
  stage: AppointmentStage
  zone: Zone
  /** 격자 0번 칸이 가리키는 시각. 서버가 정한 값이라 참가자 모두가 같다. */
  slotBaseTime: string
  slotCount: number
  /** 식별 화면에서 쓸 표시와 번호. 참가자 전원이 같은 값을 든다. */
  identityMark: number
  identityNumber: number
  mySlots: number[]
  myName: string
  myArrived: boolean
  partners: AppointmentPartner[]
  /** 모두가 되는 가장 빠른 칸. 서버가 계산해 준다. */
  overlapSlot: number | null
  allAnswered: boolean
  confirmedLabel: string | null
  /** 확정된 만나는 시각. 남은 시간을 세는 데 쓴다. */
  confirmedTime: string | null
}

/**
 * 화면에 보여줄 매칭. 자동 매칭인지 찔러보기 성사인지에 따라 제목이 달라진다.
 *
 * `exchangeId` 가 있으면 서버가 실제로 만든 교환이라 수락·거절이 `/api/exchanges`
 * 를 부른다. 목업(자동 매칭 시뮬레이션·찔러보기)은 서버에 아무것도 없어서 `null` 이다.
 */
export type ActiveMatch = MatchResult & { origin: 'auto' | 'poke'; exchangeId: number | null }

export type State = {
  onboarded: boolean
  /**
   * 교환 대기장소까지 한 번이라도 갔는지. 처음 등록 중인지 나중에 고치는 중인지를
   * 이걸로 가른다. Have 화면의 뒤로가기가 어디로 갈지가 달라진다.
   */
  setupDone: boolean
  have: Selection[]
  needs: Selection[]
  /** 자동 매칭이 돌고 있는지. 약속이 있으면 돌지 않는다. */
  autoMatching: boolean
  match: ActiveMatch | null
  /** 거절한 상대는 다시 매칭에 올리지 않는다. */
  declined: string[]
  outgoingPoke: OutgoingPoke | null
  incomingPoke: IncomingPoke | null
  /** 매칭이 도는 동안 들어온 찔러보기는 끝날 때까지 알리지 않는다. */
  heldIncoming: IncomingPoke | null
  /** 진행 중이거나 확정된 약속들. 현재 시간에서 가까운 순으로 화면에 깔린다. */
  appointments: Appointment[]
  /** 지금 장소·시간·약속 화면이 다루고 있는 약속. 서버 교환 id 다. */
  activeAppointmentId: number | null
  /** 실시간 알림을 구독하려면 부스 id 가 있어야 한다. 앱을 열 때 서버에서 읽는다. */
  boothId: number | null
  /** 이 부스의 교환 장소들. 약도 핀이 이 순서를 쓴다. */
  zones: Zone[]
  notifications: AppNotification[]
  /** 교환으로 얻은 카드 */
  collection: string[]
  toast: string | null
}
