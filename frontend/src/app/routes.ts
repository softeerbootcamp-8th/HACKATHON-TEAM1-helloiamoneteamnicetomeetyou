/**
 * 화면 순서를 한 곳에 적어 둔다. 전환 방향(앞으로/뒤로)을 이 순서로 판단한다.
 */
export const ROUTE_ORDER = [
  '/',
  '/have',
  '/needs',
  '/home',
  '/poke/confirm',
  '/poke/received',
  '/match',
  '/place',
  '/time',
  '/appointment',
  '/identify',
  '/complete',
] as const

export function routeIndex(pathname: string): number {
  const found = ROUTE_ORDER.findIndex((r) => r === pathname)
  return found === -1 ? ROUTE_ORDER.length : found
}
