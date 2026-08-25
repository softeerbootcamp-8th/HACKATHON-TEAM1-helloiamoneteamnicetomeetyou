/**
 * 교환 장소와 교환 약속 API.
 *
 * 백엔드의 `domain/booth` 와 `domain/exchange` 에 대응한다. 응답 형식이 바뀌면 양쪽을 같이 고친다.
 *
 * 부스 목록과 카드 목록, 사용자 등록은 `features/catalog/api.ts` 에 있다. 앱을 열 때 한 번 맞추는
 * 것들이라 그쪽이 먼저 부르고, 여기서는 그 결과로 받은 `boothId` 를 쓴다.
 */
import { api, apiData } from './api'

export type Zone = {
  id: number
  name: string
  location: string
}

export type ExchangeType = 'ONE_TO_ONE' | 'MULTI_WAY'

export type ExchangeStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export type ExchangeParticipant = {
  userId: string
  /** 등록할 때 이름을 안 보낸 사용자가 있어서 비어 있을 수 있다. */
  username: string | null
  slots: number[]
  answered: boolean
  arrived: boolean
}

/**
 * 약속의 현재 상태 전부다.
 *
 * `slotBaseTime` 이 격자의 0번 칸이 가리키는 시각이다. **화면이 자기 시계로 격자를 만들면 안 된다.**
 * 14:03 에 연 사람의 0번 칸은 14:15 이고 14:20 에 연 사람의 0번 칸은 14:30 이라, 같은 칸 번호가
 * 사람마다 다른 시각을 뜻하게 된다. 서버가 교환을 만들 때 한 번 정한 이 값을 모두가 함께 쓴다.
 */
export type Exchange = {
  exchangeId: number
  boothId: number
  type: ExchangeType
  status: ExchangeStatus
  zone: Zone
  /** `2026-08-25T14:15:00` 처럼 시간대가 없는 값이다. 서버와 사용자가 같은 지역에 있다고 본다. */
  slotBaseTime: string
  slotCount: number
  slotMinutes: number
  /**
   * 식별 화면에서 쓸 표시와 번호. 시안의 "레몬 28" 이다.
   *
   * 참가자 전원이 같은 값을 든다. 같은 화면을 든 사람이 내 상대라는 것이 그 화면의 규칙이다.
   * 진행 중인 다른 교환과도 겹치지 않게 서버가 골라 준다.
   */
  identityMark: number
  identityNumber: number
  participants: ExchangeParticipant[]
  /** 모두가 되는 가장 빠른 칸. 없으면 null 이다. */
  overlapSlot: number | null
  allAnswered: boolean
  confirmedTime: string | null
}

export function fetchZones(boothId: number): Promise<Zone[]> {
  return apiData<Zone[]>(`/api/booths/${boothId}/zones`)
}

/**
 * 교환을 만든다.
 *
 * 매칭이 아직 화면 쪽 목업이라 프론트가 부르는 임시 엔드포인트다. 서버가 매칭을 하게 되면
 * 이 호출은 사라지고, 교환은 매칭 결과 알림으로 내려온다.
 */
export function createExchange(input: {
  boothId: number
  type: ExchangeType
  participantUserIds: string[]
}): Promise<Exchange> {
  return apiData<Exchange>('/api/exchanges', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

/**
 * 내가 지금 잡고 있는 약속. 없으면 null 이다.
 *
 * 앱을 열 때와 실시간 연결이 붙을 때 부른다. 화면 상태가 메모리에만 있어서 새로고침 한 번에
 * 사라지는데, 이게 없으면 진행 중인 약속으로 돌아올 방법이 없다. 알림만으로는 부족한 것이,
 * 끊겨 있던 동안 온 알림은 다시 오지 않기 때문이다.
 */
export async function fetchActiveExchange(userId: string): Promise<Exchange | null> {
  // 진행 중인 약속이 없으면 서버가 204 로 답하고, api() 는 그때 undefined 를 준다.
  const res = await api<{ data?: Exchange } | undefined>(
    `/api/exchanges/active?userId=${encodeURIComponent(userId)}`,
  )

  return res?.data ?? null
}

export function fetchExchange(exchangeId: number): Promise<Exchange> {
  return apiData<Exchange>(`/api/exchanges/${exchangeId}`)
}

/** 고른 칸을 통째로 덮어쓴다. 칸 하나를 켜고 끄는 것이 아니라 항상 전체를 보낸다. */
export function updateTimeSlots(
  exchangeId: number,
  userId: string,
  slots: number[],
): Promise<Exchange> {
  return apiData<Exchange>(`/api/exchanges/${exchangeId}/time-slots`, {
    method: 'PUT',
    body: JSON.stringify({ userId, slots }),
  })
}

/** "시간 조율 요청하기". 참가자 전원의 선택을 비운다. */
export function resetTimeSlots(exchangeId: number, userId: string): Promise<Exchange> {
  return apiData<Exchange>(`/api/exchanges/${exchangeId}/time-slots/reset`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

/** "약속 확정하기". 겹치는 가장 빠른 칸으로 정한다. 참가자 중 한 명만 누르면 된다. */
export function confirmExchangeTime(exchangeId: number, userId: string): Promise<Exchange> {
  return apiData<Exchange>(`/api/exchanges/${exchangeId}/confirm-time`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

/** "도착했어요". 상대 화면의 배지가 이동중에서 도착으로 바뀐다. */
export function arriveAtExchange(exchangeId: number, userId: string): Promise<Exchange> {
  return apiData<Exchange>(`/api/exchanges/${exchangeId}/arrive`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

/**
 * "만났어요". 교환이 끝났다는 것을 서버에 남긴다.
 *
 * 한 명이 이걸 누르고 다른 한 명이 취소를 누를 수 있어서, 먼저 도착한 한 번만 반영된다.
 * 늦은 쪽은 실패를 받고 화면을 현재 상태로 맞춘다.
 *
 * 카드 주인이 바뀌는 것은 아직 여기 없다. 무엇을 주고받는지는 매칭이 정하는 값이다.
 */
export function completeExchange(exchangeId: number, userId: string): Promise<Exchange> {
  return apiData<Exchange>(`/api/exchanges/${exchangeId}/complete`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

/** 거래 취소. 상대 화면에도 취소됐다는 것이 실시간으로 전해진다. */
export function cancelExchange(exchangeId: number, userId: string): Promise<Exchange> {
  return apiData<Exchange>(`/api/exchanges/${exchangeId}/cancel`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}
