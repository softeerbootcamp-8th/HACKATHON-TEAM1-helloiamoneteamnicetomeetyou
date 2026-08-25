/**
 * 화면 순서를 한 곳에 적어 둔다. 전환 방향(앞으로/뒤로)을 이 순서로 판단하고,
 * 어떤 화면이 데스크톱에서 넓게 펼쳐지는지도 여기서 정한다.
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

/** 데스크톱에서 좌우로 펼치는 화면. 홈이 핵심 화면이라 여기만 넓다. */
export const WIDE_ROUTES = new Set<string>(['/home'])

export function routeIndex(pathname: string): number {
  const found = ROUTE_ORDER.findIndex((r) => r === pathname)
  return found === -1 ? ROUTE_ORDER.length : found
}
