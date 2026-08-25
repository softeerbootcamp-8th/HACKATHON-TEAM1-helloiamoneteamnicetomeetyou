import { formatConfirmedTime } from './time'
import type { Appointment, AppointmentStage } from './types'
import type { Exchange } from '@/lib/exchange'

/**
 * 서버가 준 교환을 화면이 쓰는 모양으로 옮긴다. 내 줄과 상대 줄을 여기서 가른다.
 *
 * 실시간 알림을 받을 때마다 서버에서 다시 읽어 이걸로 갈아끼운다. 그래서 화면은 이벤트 순서를
 * 신경 쓸 필요가 없고, 재연결로 몇 개를 놓쳐도 다음 한 번으로 맞는 상태가 된다.
 */
export function toAppointment(
  exchange: Exchange,
  myUserId: string,
  previousStage?: AppointmentStage,
): Appointment {
  const me = exchange.participants.find((p) => p.userId === myUserId)

  return {
    exchangeId: exchange.exchangeId,
    stage: stageOf(exchange, myUserId, previousStage),
    zone: exchange.zone,
    slotBaseTime: exchange.slotBaseTime,
    slotCount: exchange.slotCount,
    identityMark: exchange.identityMark,
    identityNumber: exchange.identityNumber,
    mySlots: me?.slots ?? [],
    myName: me?.username ?? '나',
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
 * 도착도 서버가 알고 있어서 `previousStage` 를 볼 필요가 없어졌다. 다른 기기에서 도착을 눌러도
 * 이 화면이 따라온다.
 */
function stageOf(
  exchange: Exchange,
  myUserId: string,
  previousStage?: AppointmentStage,
): AppointmentStage {
  const me = exchange.participants.find((p) => p.userId === myUserId)

  if (me?.arrived) return 'arrived'
  if (previousStage === 'arrived') return 'arrived'
  if (exchange.confirmedTime) return 'confirmed'
  if (exchange.allAnswered && exchange.overlapSlot === null) return 'time-conflict'
  return 'time-waiting'
}
