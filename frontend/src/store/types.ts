import type { MatchResult } from './matching'

export type Selection = { itemId: string; qty: number }

export type NotificationKind =
  'match' | 'poke-received' | 'poke-accepted' | 'poke-rejected' | 'time-request'

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

export type AppointmentStage = 'place' | 'time-waiting' | 'time-conflict' | 'confirmed' | 'arrived'

export type Appointment = {
  stage: AppointmentStage
  zoneId: string
  mySlots: number[]
  /** 상대별로 고른 칸. 아직 안 고른 상대는 키가 없다. */
  partnerSlots: Record<string, number[]>
  confirmedSlot: number | null
  confirmedLabel: string | null
}

/** 화면에 보여줄 매칭. 자동 매칭인지 찔러보기 성사인지에 따라 제목이 달라진다. */
export type ActiveMatch = MatchResult & { origin: 'auto' | 'poke' }

export type State = {
  onboarded: boolean
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
  appointment: Appointment | null
  notifications: AppNotification[]
  /** 교환으로 얻은 카드 */
  collection: string[]
  toast: string | null
}
