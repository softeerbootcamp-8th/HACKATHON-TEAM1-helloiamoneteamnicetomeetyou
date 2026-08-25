import { useCallback, useEffect, useState } from 'react'

import {
  detectPushSupport,
  enablePush,
  fetchVapidPublicKey,
  getPermission,
  sendTestPush,
  syncPush,
} from '@/lib/push'

export type PushState =
  /** 켤 수 있는지 알아보는 중 */
  | { status: 'loading' }
  /** 이 브라우저로는 안 된다. 아무것도 보여 주지 않는다 */
  | { status: 'unsupported' }
  /** iOS 사파리 탭이다. 홈 화면에 추가해야 한다 */
  | { status: 'needs-install' }
  /** 껐다. iOS 는 다시 물어보지 않으므로 설정에서 켜야 한다 */
  | { status: 'denied' }
  | { status: 'idle' }
  | { status: 'enabling' }
  | { status: 'enabled' }
  | { status: 'error'; reason: string }

/**
 * 앱을 닫아 둔 사이에도 알림을 받게 한다.
 *
 * 열려 있는 동안의 알림은 SSE 가 맡고, 서버가 SSE 연결이 없는 사람에게만 푸시를 보낸다.
 * 그래서 여기서 "지금 앱이 열려 있으니 무시" 같은 처리를 하지 않는다.
 */
/**
 * 지원 여부와 권한은 첫 렌더에 바로 알 수 있다. effect 에서 setState 로 정하면 화면이 한 번
 * 헛돌고 lint 에도 걸리므로 초기값으로 계산한다.
 */
function initialPushState(): PushState {
  const support = detectPushSupport()

  if (support !== 'available') {
    return { status: support === 'needs-install' ? 'needs-install' : 'unsupported' }
  }

  if (getPermission() === 'denied') {
    return { status: 'denied' }
  }

  // 서버에서 공개키를 받고 기존 구독을 확인해야 켜져 있는지 알 수 있다.
  return { status: 'loading' }
}

export function usePush(userId: string) {
  const [state, setState] = useState<PushState>(initialPushState)

  // 공개키는 미리 받아 둔다. 버튼을 누른 뒤에 네트워크를 기다리면 iOS 가 그 요청을
  // 사용자 제스처 밖에서 온 것으로 보고 권한 요청을 거부한다.
  const [vapidPublicKey, setVapidPublicKey] = useState<string | null>(null)

  useEffect(() => {
    // 위 초기값에서 이미 갈라진 경우다. 네트워크를 쓸 일이 없다.
    if (detectPushSupport() !== 'available') return
    if (getPermission() === 'denied') return

    let cancelled = false

    void (async () => {
      try {
        const key = await fetchVapidPublicKey()
        if (cancelled) return
        setVapidPublicKey(key)

        // 이미 켜 둔 구독이 있으면 서버에 다시 맞춰 둔다. 읽기라 제스처가 필요 없다.
        const already = await syncPush(userId)
        if (cancelled) return

        setState({ status: already ? 'enabled' : 'idle' })
      } catch (error: unknown) {
        if (cancelled) return

        // 원인을 삼키면 왜 안 켜지는지 알 방법이 없다. 화면에는 짧게 보여 주고 콘솔에 남긴다.
        console.error('[push] 초기화 실패', error)

        // 공개키를 못 받으면 켤 수가 없다. 버튼을 보여 주고 누르게 하면 실패만 반복한다.
        setState({ status: 'error', reason: '알림 설정을 불러오지 못했어요.' })
      }
    })()

    return () => {
      cancelled = true
    }
  }, [userId])

  /** 반드시 클릭 핸들러에서 부른다. */
  const enable = useCallback(async () => {
    if (!vapidPublicKey) {
      // 조용히 아무것도 안 하면 버튼이 고장 난 것처럼 보인다.
      setState({ status: 'error', reason: '알림 설정을 아직 불러오지 못했어요.' })
      return
    }

    setState({ status: 'enabling' })

    try {
      await enablePush(userId, vapidPublicKey)
      setState({ status: 'enabled' })

      // 켜자마자 한 번 보내 준다. 실제로 오는 것을 봐야 켜졌다고 믿을 수 있다.
      await sendTestPush(userId)
    } catch (error: unknown) {
      console.error('[push] 켜기 실패', error)

      // 권한 팝업에서 거부한 경우와 그 밖의 실패를 나눠 보여 준다.
      if (getPermission() === 'denied') {
        setState({ status: 'denied' })
        return
      }

      // 무엇 때문에 막혔는지 화면에서도 보이게 한다. 현장에서 콘솔을 열 수 없다.
      const reason = error instanceof Error ? error.message : '알 수 없는 오류'
      setState({ status: 'error', reason: `알림을 켜지 못했어요. (${reason})` })
    }
  }, [userId, vapidPublicKey])

  return { state, enable }
}
