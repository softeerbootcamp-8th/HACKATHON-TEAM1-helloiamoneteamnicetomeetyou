/**
 * 부스, 교환 장소, 교환 약속 API.
 *
 * 백엔드의 `domain/booth` 와 `domain/exchange` 에 대응한다. 응답 형식이 바뀌면 양쪽을 같이 고친다.
 */
import { api, type CommonResponse } from './api'

export type Zone = {
  id: number
  name: string
  location: string
}

export type Booth = {
  id: number
  name: string
  description: string | null
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

/** 응답 껍데기를 벗긴다. `api()` 는 이걸 대신해 주지 않는다. */
function unwrap<T>(res: CommonResponse<T>): T {
  if (!res.success || res.data === undefined) {
    throw new Error(res.message ?? '요청에 실패했어요')
  }
  return res.data
}

export async function fetchBooths(): Promise<Booth[]> {
  return unwrap(await api<CommonResponse<Booth[]>>('/api/booths'))
}

export async function fetchZones(boothId: number): Promise<Zone[]> {
  return unwrap(await api<CommonResponse<Zone[]>>(`/api/booths/${boothId}/zones`))
}

/** 앱을 열 때마다 불러도 되는 멱등 등록이다. 이름은 상대 화면에 보여줄 라벨이다. */
export async function registerUser(userId: string, username: string): Promise<void> {
  await api<CommonResponse<void>>('/api/users', {
    method: 'POST',
    body: JSON.stringify({ userId, username }),
  })
}

/**
 * 교환을 만든다.
 *
 * 매칭이 아직 화면 쪽 목업이라 프론트가 부르는 임시 엔드포인트다. 서버가 매칭을 하게 되면
 * 이 호출은 사라지고, 교환은 매칭 결과 알림으로 내려온다.
 */
export async function createExchange(input: {
  boothId: number
  type: ExchangeType
  participantUserIds: string[]
}): Promise<Exchange> {
  return unwrap(
    await api<CommonResponse<Exchange>>('/api/exchanges', {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  )
}

export async function fetchExchange(exchangeId: number): Promise<Exchange> {
  return unwrap(await api<CommonResponse<Exchange>>(`/api/exchanges/${exchangeId}`))
}

/** 고른 칸을 통째로 덮어쓴다. 칸 하나를 켜고 끄는 것이 아니라 항상 전체를 보낸다. */
export async function updateTimeSlots(
  exchangeId: number,
  userId: string,
  slots: number[],
): Promise<Exchange> {
  return unwrap(
    await api<CommonResponse<Exchange>>(`/api/exchanges/${exchangeId}/time-slots`, {
      method: 'PUT',
      body: JSON.stringify({ userId, slots }),
    }),
  )
}

/** "시간 조율 요청하기". 참가자 전원의 선택을 비운다. */
export async function resetTimeSlots(exchangeId: number, userId: string): Promise<Exchange> {
  return unwrap(
    await api<CommonResponse<Exchange>>(`/api/exchanges/${exchangeId}/time-slots/reset`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    }),
  )
}

/** "도착했어요". 상대 화면의 배지가 이동중에서 도착으로 바뀐다. */
export async function arriveAtExchange(exchangeId: number, userId: string): Promise<Exchange> {
  return unwrap(
    await api<CommonResponse<Exchange>>(`/api/exchanges/${exchangeId}/arrive`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    }),
  )
}

/** 거래 취소. 상대 화면에도 취소됐다는 것이 실시간으로 전해진다. */
export async function cancelExchange(exchangeId: number, userId: string): Promise<Exchange> {
  return unwrap(
    await api<CommonResponse<Exchange>>(`/api/exchanges/${exchangeId}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    }),
  )
}

/** "약속 확정하기". 겹치는 가장 빠른 칸으로 정한다. 참가자 중 한 명만 누르면 된다. */
export async function confirmExchangeTime(exchangeId: number, userId: string): Promise<Exchange> {
  return unwrap(
    await api<CommonResponse<Exchange>>(`/api/exchanges/${exchangeId}/confirm-time`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    }),
  )
}
