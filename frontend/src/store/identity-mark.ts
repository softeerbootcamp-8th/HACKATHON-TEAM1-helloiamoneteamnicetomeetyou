/**
 * 식별 화면에서 서로를 찾는 표시.
 *
 * 서버는 교환마다 번호 하나만 정해 주고, 그것을 무엇으로 보여줄지는 화면이 정한다. 지도 위 핀
 * 좌표를 화면이 정하는 것과 같은 이유다. 표시를 바꿔도 서버를 안 고쳐도 된다.
 *
 * **시안이 정한 5종이다.** 레몬, 사과, 체리, 수박, 복숭아.
 * 가짓수를 바꾸려면 백엔드 `ExchangeService.IDENTITY_MARK_COUNT` 도 같이 바꿔야 한다.
 * 여기보다 서버 쪽이 크면 표에 없는 번호가 내려와서 레몬으로 되돌아간다.
 *
 * 그림은 `public/lemon.svg` 하나를 쓰고 색조만 돌린다. 과일 그림을 종류마다 그리는 것은 시안에
 * 없는 자산을 만드는 일이라, 시안이 준 레몬을 그대로 두고 색으로 갈랐다. 실제 과일 아이콘이
 * 들어오면 여기 표에 파일 경로를 더하면 된다.
 */
export type IdentityMark = {
  name: string
  /** `lemon.svg` 의 노란색을 몇 도 돌릴지 */
  hueRotate: number
}

export const IDENTITY_MARKS: IdentityMark[] = [
  { name: '레몬', hueRotate: 0 },
  { name: '사과', hueRotate: -45 },
  { name: '체리', hueRotate: -80 },
  { name: '수박', hueRotate: 110 },
  { name: '복숭아', hueRotate: -18 },
]

/** 표에 없는 번호가 와도 화면이 비지 않게 첫 표시로 되돌린다. */
export function identityMarkAt(index: number): IdentityMark {
  return IDENTITY_MARKS[index] ?? IDENTITY_MARKS[0]
}

/**
 * 시안의 "레몬 28" 자리.
 *
 * 교환 하나에 하나뿐이고 참가자 전원이 같은 값을 본다. 진행 중인 다른 교환과 겹치지 않게
 * 서버가 골라 주기 때문에, 같은 글자를 든 사람이 곧 내 교환 상대다.
 */
export function identityLabel(markIndex: number, number: number): string {
  return `${identityMarkAt(markIndex).name} ${number}`
}
