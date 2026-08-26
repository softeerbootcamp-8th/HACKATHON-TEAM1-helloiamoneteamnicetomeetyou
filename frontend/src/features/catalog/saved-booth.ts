/**
 * 고른 부스를 다음 방문까지 들고 있는다.
 *
 * 행사장에서 화면을 새로고침하거나 PWA 를 다시 여는 일이 잦은데, 그때마다 첫 부스로
 * 돌아가면 시연 도중에 엉뚱한 부스를 보게 된다.
 *
 * 저장소를 못 쓰면 그냥 기억하지 않는다. 부스는 랜딩에서 다시 고르면 되는 값이라
 * `identity.ts` 의 기기 식별자처럼 메모리로 버텨 줄 이유가 없다.
 */
const KEY = 'tradit.boothId'

export function readSavedBoothId(): number | null {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved === null) return null
    const id = Number(saved)
    return Number.isInteger(id) ? id : null
  } catch {
    return null
  }
}

export function saveBoothId(boothId: number): void {
  try {
    localStorage.setItem(KEY, String(boothId))
  } catch {
    // 사파리 프라이빗 모드. 이번 방문 동안은 state 가 들고 있으니 화면은 그대로 돈다.
  }
}

export function forgetBoothId(): void {
  try {
    localStorage.removeItem(KEY)
  } catch {
    // 위와 같다.
  }
}
