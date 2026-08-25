/**
 * 짧은 진동. 안드로이드 크롬에서만 실제로 울리고 iOS 사파리는 무시하지만,
 * 되는 기기에서는 끌어놓기가 붙는 느낌이 확 달라진다.
 */
export function tick(pattern: number | number[] = 8) {
  try {
    navigator.vibrate?.(pattern)
  } catch {
    // 진동이 막힌 환경에서도 화면은 그대로 동작해야 한다.
  }
}
