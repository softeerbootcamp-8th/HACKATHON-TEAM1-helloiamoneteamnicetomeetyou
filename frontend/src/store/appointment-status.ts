import { itemById } from '@/mocks/data'

import type { Appointment } from './types'

export type AppointmentStatus = {
  id: string
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
  const pair = `${itemById(match.giveItemId).name}↔${itemById(match.receiveItemId).name}`

  if (appointment.stage === 'confirmed' || appointment.stage === 'arrived') {
    return {
      id: appointment.id,
      to: '/appointment',
      title: `${appointment.confirmedLabel}에 만나요`,
      sub: '모두 되는 가장 빠른 시간',
    }
  }
  if (appointment.mySlots.length === 0) {
    return { id: appointment.id, to: '/time', title: '가능한 시간을 입력해주세요', sub: pair }
  }
  return { id: appointment.id, to: '/time', title: '시간 조율 중이에요', sub: pair }
}

/**
 * 현재 시간에서 가까운 약속이 앞에 온다.
 * 아직 시간이 안 정해진 약속은 비교할 시각이 없어서 뒤로 민다.
 */
export function sortedAppointments(appointments: Appointment[]): Appointment[] {
  return [...appointments].sort((a, b) => {
    if (a.confirmedSlot === null && b.confirmedSlot === null) return 0
    if (a.confirmedSlot === null) return 1
    if (b.confirmedSlot === null) return -1
    return a.confirmedSlot - b.confirmedSlot
  })
}
