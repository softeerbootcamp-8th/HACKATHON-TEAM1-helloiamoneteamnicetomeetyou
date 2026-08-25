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
 * 지금 시각을 15분 단위로 올림해서 격자를 만든다. 지나간 시간은 아예 만들지 않으므로
 * 과거 칸이 화면에 남는 일이 없다.
 */
export function buildSlots(from: Date): Slot[] {
  const base = new Date(from)
  base.setSeconds(0, 0)
  const remainder = base.getMinutes() % SLOT_MINUTES
  if (remainder !== 0) base.setMinutes(base.getMinutes() + (SLOT_MINUTES - remainder))

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
export function slotTimeLabel(slots: Slot[], index: number, from: Date): string {
  if (index !== 0) return slots[index].label
  const base = new Date(from)
  base.setSeconds(0, 0)
  const remainder = base.getMinutes() % SLOT_MINUTES
  if (remainder !== 0) base.setMinutes(base.getMinutes() + (SLOT_MINUTES - remainder))
  return `${base.getHours()}:${pad(base.getMinutes())}`
}

/** 모두가 되는 가장 빠른 칸을 찾는다. 없으면 -1 이다. */
export function earliestOverlap(rows: number[][]): number {
  if (rows.length === 0) return -1
  for (let i = 0; i < SLOT_COUNT; i += 1) {
    if (rows.every((row) => row.includes(i))) return i
  }
  return -1
}
