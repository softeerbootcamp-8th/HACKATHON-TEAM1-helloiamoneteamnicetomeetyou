import { useEffect } from 'react'

/**
 * 식별 화면에서 서로를 찾는 표시.
 *
 * 서버는 교환마다 번호 하나만 정해 주고, 그것을 무엇으로 보여줄지는 화면이 정한다. 지도 위 핀
 * 좌표를 화면이 정하는 것과 같은 이유다. 표시를 바꿔도 서버를 안 고쳐도 된다.
 *
 * **5종이다.** 레몬, 포도, 체리, 수박, 복숭아.
 * 가짓수를 바꾸려면 백엔드 `ExchangeService.IDENTITY_MARK_COUNT` 도 같이 바꿔야 한다.
 * 여기보다 서버 쪽이 크면 표에 없는 번호가 내려와서 레몬으로 되돌아간다.
 *
 * 그림은 굿즈 카드와 같은 Supabase 버킷에서 온다. 예전에는 `lemon.svg` 하나를 색조만 돌려
 * 썼는데, 그러면 레몬 모양에 색만 입힌 것이라 멀리서 보면 포도인지 체리인지 알 수 없다.
 * 이 화면은 사람이 몰린 곳에서 서로를 알아보라고 있는 자리라 모양이 달라야 한다.
 */
const IMAGE_BASE = 'https://sdumqvkniemiowanvsef.supabase.co/storage/v1/object/public/items/fruits'

/**
 * 그림 주소 끝에 붙는 번호. **버킷의 그림을 같은 이름으로 갈아 끼웠으면 이 숫자를 올린다.**
 *
 * 서비스 워커가 이 버킷을 CacheFirst 로 30일 들고 있는다(`sw.ts`). 주소가 그대로면 그림만
 * 바꿔 올려도 이미 앱을 켰던 사람은 30일 내내 옛 그림을 본다. 실제로 새 과일을 올린 뒤에도
 * 기기에 따라 이전 과일이 떠서, 같은 교환을 하는 두 사람이 서로 다른 그림을 들고 있었다.
 * 숫자가 바뀌면 주소가 바뀌므로 캐시가 새로 받는다.
 */
const IMAGE_VERSION = 2

export type IdentityMark = {
  name: string
  /** 그림 주소. 파일 이름과 버전이 곧 내용이라 캐시에 그대로 얹힌다. */
  src: string
}

function mark(name: string, file: string): IdentityMark {
  return { name, src: `${IMAGE_BASE}/${file}.webp?v=${IMAGE_VERSION}` }
}

export const IDENTITY_MARKS: IdentityMark[] = [
  mark('레몬', 'lemon'),
  mark('포도', 'grape'),
  mark('체리', 'cherry'),
  mark('수박', 'watermelon'),
  mark('복숭아', 'peach'),
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

/**
 * 받침이 있으면 '을', 없으면 '를'.
 *
 * 표시 이름이 고정이 아니라서 조사도 따라 바뀌어야 한다. '포도을 찾아볼까요' 가 화면에
 * 나오면 공들인 시안이 한 번에 싸구려가 된다.
 */
export function objectParticle(word: string): '을' | '를' {
  const last = word.charCodeAt(word.length - 1)
  const isHangulSyllable = last >= 0xac00 && last <= 0xd7a3
  // 한글 음절은 (초성, 중성, 종성) 순서로 쌓여 있어서 28 로 나눈 나머지가 종성이다.
  return isHangulSyllable && (last - 0xac00) % 28 !== 0 ? '을' : '를'
}

/**
 * 이 교환의 표시 그림을 미리 받아 둔다.
 *
 * 정작 쓰는 곳은 교환 장소에서 서로를 찾는 화면인데, 거기는 사람이 몰려서 네트워크가 제일
 * 안 좋고 그림이 화면 내용의 전부다. 약속이 잡힌 시점은 아직 시간이 남아 있고 사용자가 폰을
 * 보고 있어서, 그때 받아 두면 정작 급할 때는 캐시에서 바로 뜬다.
 *
 * 약속이 없으면(`null`) 아무것도 하지 않는다. 어느 그림이 필요한지 아직 모른다.
 */
export function usePrefetchMark(markIndex: number | null): void {
  const src = markIndex === null ? null : identityMarkAt(markIndex).src

  useEffect(() => {
    if (!src) return
    // 브라우저 캐시에만 올려 두면 되어서 결과를 쓰지 않는다. 실패해도 화면에서 다시
    // 부르기 때문에 조용히 넘어간다.
    const img = new Image()
    // crossOrigin 을 안 붙이면 no-cors 로 나가서 응답이 opaque 로 온다. opaque 는 성공이든
    // 404 든 status 가 0 이라, 서비스 워커가 실패한 응답을 성공으로 알고 캐시에 굳혀 버린다.
    // 한 번 그렇게 되면 그 기기에서는 그림이 영영 안 뜬다. 버킷이 `access-control-allow-origin: *`
    // 를 주기 때문에 이걸 붙이면 실제 status 가 그대로 온다.
    img.crossOrigin = 'anonymous'
    img.src = src
  }, [src])
}
