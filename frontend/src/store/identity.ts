/**
 * 로그인 없이 기기마다 고정된 식별자를 만든다. 백엔드 인증 방식이 정해지기 전까지의
 * 자리표시자이고, 정해지면 이 파일만 바꾸면 된다.
 *
 * 저장소가 비면 새 식별자가 되는 것은 알고 있다. 행사장에서 잠깐 쓰는 서비스라
 * 지금은 이 정도로 충분하다고 보고 넘어간다.
 */
const KEY = 'tradit.deviceId'

/**
 * 저장소를 못 쓸 때 이번 방문 동안만 들고 있을 값이다.
 *
 * 예전에는 고정 문자열 'anonymous' 를 돌려줬는데, 그러면 프라이빗 모드로 들어온 사람들이
 * 전부 같은 사용자가 되어 서로의 알림과 교환을 받게 된다. UUID 형식도 아니라 서버가 거절한다.
 */
let memoryFallback: string | null = null

export function getDeviceId(): string {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved) return saved

    const fresh = crypto.randomUUID()
    localStorage.setItem(KEY, fresh)
    return fresh
  } catch {
    // 사파리 프라이빗 모드처럼 저장소가 막힌 경우에도 화면은 떠야 한다.
    // 새로고침하면 다른 사람이 되지만, 한 방문 안에서는 일관되게 동작한다.
    memoryFallback ??= crypto.randomUUID()
    return memoryFallback
  }
}

/**
 * 서버에 등록할 내 이름. 상대 화면에서 내 줄의 라벨이 된다.
 *
 * **기기마다 달라야 한다.** 고정값으로 두면 두 기기를 붙였을 때 상대 줄에도 내 이름과 같은
 * 글자가 뜨는데, 그러면 누가 누구인지 화면에서 가릴 수가 없다.
 *
 * 식별 화면의 "레몬 28" 과는 다른 값이다. 그쪽은 교환마다 서버가 정해 준다.
 */
export function myUsername(deviceId: string): string {
  let hash = 0
  for (const ch of deviceId) hash = (hash * 31 + ch.charCodeAt(0)) | 0
  return `손님 ${String((Math.abs(hash) % 90) + 10)}`
}
