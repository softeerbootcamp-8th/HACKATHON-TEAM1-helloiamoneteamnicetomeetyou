import { itemById } from '@/mocks/data'

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
 */
export function appointmentStatus(appointment: Appointment): AppointmentStatus {
  const { match } = appointment
  // 무엇을 주고받는지는 아직 화면 목업이 아는 값이라, 없으면 상대 이름으로 대신한다.
  const pair = match
    ? `${itemById(match.giveItemId).name}↔${itemById(match.receiveItemId).name}`
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
