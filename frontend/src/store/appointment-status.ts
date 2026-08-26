import type { Item } from '@/features/catalog/api'

import type { Appointment } from './types'

export type AppointmentStatus = {
  id: number
  /** 눌렀을 때 갈 화면 */
  to: string
  title: string
  sub: string
}

/**
 * 교환 대기장소 위쪽에 뜨는 약속 상태. 시안의 `교환 대기장소 약속 상태 표시` 다.
 * 어느 단계에 있는지에 따라 문구와 눌렀을 때 가는 곳이 갈린다.
 *
 * 카드를 찾는 함수를 받아 쓴다. 훅이 아니라 목록을 만드는 자리에서 여러 번 불리는 함수라
 * 컨텍스트를 직접 읽을 수 없다.
 */
export function appointmentStatus(
  appointment: Appointment,
  itemById: (itemId: number) => Item | undefined,
): AppointmentStatus {
  const { match } = appointment
  // 서버는 무엇을 주고받는지 모른다. 매칭을 거치지 않고 들어온 약속이면 상대 이름으로 대신한다.
  const nameOf = (itemId: number) => itemById(itemId)?.name ?? '알 수 없는 카드'
  const pair = match
    ? `${nameOf(match.giveItemId)}↔${nameOf(match.receiveItemId)}`
    : appointment.partners.map((p) => p.name).join(', ')

  if (appointment.stage === 'confirmed' || appointment.stage === 'arrived') {
    return {
      id: appointment.exchangeId,
      to: '/appointment',
      title: `${appointment.confirmedLabel}에 만나요`,
      sub: '모두 되는 가장 빠른 시간',
    }
  }
  if (appointment.mySlots.length === 0) {
    return {
      id: appointment.exchangeId,
      to: '/time',
      title: '가능한 시간을 입력해주세요',
      sub: pair,
    }
  }
  return { id: appointment.exchangeId, to: '/time', title: '시간 조율 중이에요', sub: pair }
}

/**
 * 현재 시간에서 가까운 약속이 앞에 온다.
 * 아직 시간이 안 정해진 약속은 비교할 시각이 없어서 뒤로 민다.
 */
export function sortedAppointments(appointments: Appointment[]): Appointment[] {
  return [...appointments].sort((a, b) => {
    if (a.confirmedTime === null && b.confirmedTime === null) return 0
    if (a.confirmedTime === null) return 1
    if (b.confirmedTime === null) return -1
    return a.confirmedTime.localeCompare(b.confirmedTime)
  })
}
