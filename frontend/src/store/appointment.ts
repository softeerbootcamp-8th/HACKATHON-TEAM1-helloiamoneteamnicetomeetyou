import { formatConfirmedTime } from './time'
import type { ActiveMatch, Appointment, AppointmentStage } from './types'
import type { Exchange } from '@/lib/exchange'

/**
 * 서버가 준 교환을 화면이 쓰는 모양으로 옮긴다. 내 줄과 상대 줄을 여기서 가른다.
 *
 * 실시간 알림을 받을 때마다 서버에서 다시 읽어 이걸로 갈아끼운다. 그래서 화면은 이벤트 순서를
 * 신경 쓸 필요가 없고, 재연결로 몇 개를 놓쳐도 다음 한 번으로 맞는 상태가 된다.
 *
 * `previous` 는 이미 들고 있던 같은 약속이다. 무엇을 주고받는지는 서버가 모르는 값이라, 화면이
 * 갖고 있던 매칭 결과를 잃지 않으려고 넘겨받는다.
 */
export function toAppointment(
  exchange: Exchange,
  myUserId: string,
  previous?: Appointment,
  match?: ActiveMatch | null,
): Appointment {
  const me = exchange.participants.find((p) => p.userId === myUserId)

  return {
    exchangeId: exchange.exchangeId,
    match: match ?? previous?.match ?? null,
    stage: stageOf(exchange, myUserId, previous?.stage),
    zone: exchange.zone,
    slotBaseTime: exchange.slotBaseTime,
    slotCount: exchange.slotCount,
    identityMark: exchange.identityMark,
    identityNumber: exchange.identityNumber,
    mySlots: me?.slots ?? [],
    myName: me?.username ?? '나',
    myTimeConfirmed: me?.timeConfirmed ?? false,
    myArrived: me?.arrived ?? false,
    partners: exchange.participants
      .filter((p) => p.userId !== myUserId)
      .map((p) => ({
        userId: p.userId,
        name: p.username ?? '상대',
        slots: p.slots,
        arrived: p.arrived,
      })),
    overlapSlot: exchange.overlapSlot,
    allAnswered: exchange.allAnswered,
    confirmedLabel: exchange.confirmedTime ? formatConfirmedTime(exchange.confirmedTime) : null,
    confirmedTime: exchange.confirmedTime,
  }
}

/**
 * 단계는 서버 상태에서 끌어낸다. 화면이 따로 들고 있으면 실시간으로 갱신될 때마다 어긋난다.
 *
 * 도착도 서버가 알고 있어서 이전 단계를 볼 필요가 거의 없지만, 완료 화면으로 넘어가는 도중처럼
 * 잠깐 어긋나는 순간이 있어서 한 번 더 본다.
 */
function stageOf(
  exchange: Exchange,
  myUserId: string,
  previousStage?: AppointmentStage,
): AppointmentStage {
  const me = exchange.participants.find((p) => p.userId === myUserId)

  // 상대가 먼저 "만났어요" 를 눌렀을 수 있다. 그때 내 화면도 완료로 따라가야 한다.
  if (exchange.status === 'COMPLETED') return 'completed'
  if (me?.arrived || previousStage === 'arrived') return 'arrived'
  if (exchange.confirmedTime) return 'confirmed'
  if (exchange.allAnswered && exchange.overlapSlot === null) return 'time-conflict'
  return 'time-waiting'
}
