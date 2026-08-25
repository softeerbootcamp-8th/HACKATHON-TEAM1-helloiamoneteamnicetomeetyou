import { api, type CommonResponse } from '@/lib/api'

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
 * 이 기기의 식별자를 서버에 등록한다. 이미 있으면 서버가 아무것도 하지 않는다(멱등).
 *
 * 앱을 열 때 한 번 부른다. 이게 없으면 서버에 사용자 행이 없어서, 푸시 구독처럼 사용자를
 * 참조하는 것들이 저장되지 않는다.
 *
 * 실패해도 화면을 막지 않는다. 백엔드가 안 떠 있어도 목업 흐름은 그대로 돌아야 한다.
 */
export async function registerDevice(userId: string): Promise<void> {
  await api<CommonResponse<void>>('/api/users', {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}
