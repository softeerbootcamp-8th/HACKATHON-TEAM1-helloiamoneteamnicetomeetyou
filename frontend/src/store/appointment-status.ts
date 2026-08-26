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

  /*
    끝난 약속. **이 분기가 없으면 맨 아래 "시간 조율 중이에요" 로 떨어진다.**

    상대가 먼저 "만났어요" 를 누르면 EXCHANGE_COMPLETED 가 `exchange-synced` 로 들어와
    단계만 완료로 바뀌고 목록에는 남는다. 목록에서 빼는 것은 `/complete` 화면이 도는
    `complete` 액션인데, 내가 그 화면을 지나지 않으면 부를 사람이 없다. 그래서 끝난 약속이
    홈 배너에 시간을 고르러 가라고 남아 있었다.

    눌렀을 때 `/complete` 로 보내는 것이 그 자리를 푸는 길이기도 하다. 그 화면이 카드를
    정리하고 목록에서 약속을 뺀다.
  */
  if (appointment.stage === 'completed') {
    return {
      id: appointment.exchangeId,
      to: '/complete',
      title: '교환이 완료됐어요',
      sub: pair,
    }
  }
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
