/**
 * 식별 화면에서 서로를 찾는 표시.
 *
 * 서버는 교환마다 번호 하나만 정해 주고, 그것을 무엇으로 보여줄지는 화면이 정한다. 지도 위 핀
 * 좌표를 화면이 정하는 것과 같은 이유다. 표시를 바꿔도 서버를 안 고쳐도 된다.
 *
 * **가짓수를 늘리려면 백엔드 `ExchangeService.IDENTITY_MARK_COUNT` 도 같이 늘려야 한다.**
 * 여기보다 서버 쪽이 크면 표에 없는 번호가 내려와서 레몬으로 되돌아간다.
 *
 * 그림은 `public/lemon.svg` 하나를 쓰고 색조만 돌린다. 과일 그림을 종류마다 그리는 것은 시안에
 * 없는 자산을 만드는 일이라, 시안이 준 레몬을 그대로 두고 색으로 갈랐다.
 */
export type IdentityMark = {
  name: string
  /** `lemon.svg` 의 노란색을 몇 도 돌릴지 */
  hueRotate: number
}

export const IDENTITY_MARKS: IdentityMark[] = [
  { name: '레몬', hueRotate: 0 },
  { name: '라임', hueRotate: 55 },
  { name: '오렌지', hueRotate: -25 },
  { name: '자몽', hueRotate: -45 },
  { name: '체리', hueRotate: -70 },
  { name: '포도', hueRotate: 230 },
  { name: '블루베리', hueRotate: 190 },
  { name: '청포도', hueRotate: 80 },
]

/** 표에 없는 번호가 와도 화면이 비지 않게 첫 표시로 되돌린다. */
export function identityMarkAt(index: number): IdentityMark {
  return IDENTITY_MARKS[index] ?? IDENTITY_MARKS[0]
}

/** 시안의 "레몬 28" 자리. */
export function identityLabel(markIndex: number, number: number): string {
  return `${identityMarkAt(markIndex).name} ${number}`
}
