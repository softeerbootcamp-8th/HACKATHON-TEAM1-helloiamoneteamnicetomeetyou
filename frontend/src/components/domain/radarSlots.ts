/**
 * 레이더 위 다섯 자리. 바텀시트가 아래를 덮기 때문에 아래쪽 두 자리는 그 위에서
 * 끝나도록 잡았다. 값은 레이더 영역 기준 백분율이다.
 */
export type RadarSlot = {
  top?: string
  bottom?: string
  left?: string
  right?: string
  /** translate 로 미세 조정하는 값 */
  x: string
  y: string
}

export const RADAR_SLOTS: RadarSlot[] = [
  { top: '4%', left: '50%', x: '-50%', y: '0%' },
  { top: '26%', left: '5%', x: '0%', y: '0%' },
  { top: '22%', right: '4%', x: '0%', y: '0%' },
  { top: '58%', left: '9%', x: '0%', y: '0%' },
  { top: '54%', right: '7%', x: '0%', y: '0%' },
]
