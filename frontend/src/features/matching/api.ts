import { apiVoid } from '@/lib/api'

/**
 * 매칭 결과를 보고 장소를 잡으러 들어간다. 서버 교환을 PENDING 에서 IN_PROGRESS 로 옮긴다.
 *
 * 목업 매칭(`exchangeId` 가 없는 매칭)에는 부를 게 없다. 호출하는 쪽에서 이미 걸러서 부른다.
 */
export function acceptExchange(
  exchangeId: number,
  userId: string,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid(`/api/exchanges/${exchangeId}/accept`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
    signal,
  })
}

/** 매칭 결과를 거절한다. 서버가 이 교환을 취소하고 나머지 참가자를 재매칭 후보로 돌린다. */
export function rejectExchange(
  exchangeId: number,
  userId: string,
  signal?: AbortSignal,
): Promise<void> {
  return apiVoid(`/api/exchanges/${exchangeId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
    signal,
  })
}
