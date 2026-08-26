/**
 * 온보딩을 한 번 마쳤는지를 기기에 기억해 둔다.
 *
 * 화면 상태는 새로고침하면 통째로 사라져서, 이미 홈까지 가 본 사람도 다시 들어오면 처음
 * 화면(부스 고르기)부터 보게 된다. 홈에서도 카드는 얼마든지 고칠 수 있어서 다시 보여줄 이유가
 * 없다. `getDeviceId` 와 같은 이유로 로그인이 없어 기기별 로컬스토리지가 유일한 기준이다.
 */
const KEY = 'tradit.setupDone'

export function getPersistedSetupDone(): boolean {
  try {
    return localStorage.getItem(KEY) === '1'
  } catch {
    // 사파리 프라이빗 모드처럼 저장소가 막혀도 화면은 떠야 한다. 그 방문 동안은 매번
    // 온보딩부터 보게 되는데, 저장이 안 되니 어쩔 수 없다.
    return false
  }
}

export function persistSetupDone(): void {
  try {
    localStorage.setItem(KEY, '1')
  } catch {
    // 저장이 안 돼도 이번 방문의 화면 흐름에는 지장이 없다.
  }
}
