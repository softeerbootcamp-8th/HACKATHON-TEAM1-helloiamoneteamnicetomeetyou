/**
 * 로그인 없이 기기마다 고정된 식별자를 만든다. 백엔드 인증 방식이 정해지기 전까지의
 * 자리표시자이고, 정해지면 이 파일만 바꾸면 된다.
 *
 * 저장소가 비면 새 식별자가 되는 것은 알고 있다. 행사장에서 잠깐 쓰는 서비스라
 * 지금은 이 정도로 충분하다고 보고 넘어간다.
 */
const KEY = 'tradit.deviceId'

export function getDeviceId(): string {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved) return saved
    const fresh = crypto.randomUUID()
    localStorage.setItem(KEY, fresh)
    return fresh
  } catch {
    // 사파리 프라이빗 모드처럼 저장소가 막힌 경우에도 화면은 떠야 한다.
    return 'anonymous'
  }
}
