import { api, apiVoid } from '@/lib/api'

import type { ServerMatchSuggested } from './from-server-match'

/**
 * 매칭 결과를 보고 장소를 잡으러 들어간다. 서버 교환을 PENDING 에서 IN_PROGRESS 로 옮긴다.
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

/**
 * 내게 온, 아직 수락하지 않은 매칭 제안. 없으면 `null` 이다.
 *
 * 실시간 연결이 붙을 때 부른다. 끊겨 있던 동안 온 `MATCH_SUGGESTED` 는 다시 오지 않아서,
 * 이걸 읽지 않으면 재연결한 화면이 자기에게 온 제안을 영영 못 본다.
 *
 * 응답은 `MATCH_SUGGESTED` 이벤트 데이터와 같은 모양이라 `fromServerMatch` 를 그대로 쓴다.
 */
export async function fetchPendingMatch(userId: string): Promise<ServerMatchSuggested | null> {
  // 대기 중인 제안이 없으면 서버가 204 로 답하고, api() 는 그때 undefined 를 준다.
  const res = await api<{ data?: ServerMatchSuggested } | undefined>(
    `/api/matches/pending?userId=${encodeURIComponent(userId)}`,
  )

  return res?.data ?? null
}
