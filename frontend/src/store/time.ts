/**
 * 시간 선택 격자. 시안이 "지금" 칸 하나에 15분 간격 7칸을 붙여서 8칸을 보여주고,
 * 안내 문구도 "오늘 지금부터 2시간까지만 고를 수 있어요" 라 그 폭에 맞췄다.
 */

export type Slot = {
  /** 격자에서의 자리 */
  index: number
  /** 화면에 찍히는 라벨 */
  label: string
  /** 첫 칸은 "지금" 으로 표시한다 */
  isNow: boolean
}

export const SLOT_COUNT = 8
const SLOT_MINUTES = 15

function pad(n: number) {
  return String(n).padStart(2, '0')
}

/**
 * 서버가 준 `2026-08-25T14:15:00` 을 Date 로 바꾼다.
 *
 * 시간대가 없는 값이라 브라우저가 이걸 현지 시각으로 읽는다. 서버와 사용자가 모두 한국에 있는
 * 행사장 서비스라 지금은 그것이 맞다. 다른 시간대에서 쓰게 되면 서버가 오프셋을 붙여야 한다.
 */
export function parseSlotBaseTime(iso: string): Date {
  return new Date(iso)
}

/**
 * 시작점에서 15분씩 떨어진 8칸을 만든다.
 *
 * **시작점은 서버가 정한다.** 화면이 각자 `new Date()` 로 만들면 14:03 에 연 사람의 0번 칸은
 * 14:15 이고 14:20 에 연 사람의 0번 칸은 14:30 이라, 같은 칸 번호가 사람마다 다른 시각을 뜻하게
 * 된다. 이미 15분 경계라 여기서 반올림하지 않는다.
 */
export function buildSlots(baseTime: Date): Slot[] {
  const base = baseTime

  return Array.from({ length: SLOT_COUNT }, (_, index) => {
    const at = new Date(base.getTime() + index * SLOT_MINUTES * 60_000)
    return {
      index,
      label: index === 0 ? '지금' : `${at.getHours()}:${pad(at.getMinutes())}`,
      isNow: index === 0,
    }
  })
}

/** 실제 시각 라벨. "지금" 칸도 약속 표시에는 시각으로 적어야 한다. */
export function slotTimeLabel(baseTime: Date, index: number): string {
  const at = new Date(baseTime.getTime() + index * SLOT_MINUTES * 60_000)
  return `${at.getHours()}:${pad(at.getMinutes())}`
}

/** 모두가 되는 칸 전부. 시안이 이 칸들 아래에 밑줄을 긋는다. */
export function overlappingSlots(rows: number[][]): number[] {
  if (rows.length === 0) return []
  return Array.from({ length: SLOT_COUNT }, (_, i) => i).filter((i) =>
    rows.every((row) => row.includes(i)),
  )
}

/** 모두가 되는 가장 빠른 칸을 찾는다. 없으면 -1 이다. */
export function earliestOverlap(rows: number[][]): number {
  return overlappingSlots(rows)[0] ?? -1
}

/** 서버가 확정해 준 시각(`2026-08-25T14:15:00`)을 화면 라벨로 바꾼다. */
export function formatConfirmedTime(iso: string): string {
  const at = new Date(iso)
  return `${at.getHours()}:${pad(at.getMinutes())}`
}
