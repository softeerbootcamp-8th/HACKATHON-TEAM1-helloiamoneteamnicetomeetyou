import { AnimatePresence, motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'

import { Toast } from '@/components/ui/Toast'
import { useCatalog } from '@/features/catalog/useCatalog'
import { fetchPendingMatch } from '@/features/matching/api'
import { fromServerMatch, type ServerMatchSuggested } from '@/features/matching/from-server-match'
import { usePokeSync } from '@/features/poke/use-poke-sync'
import { useBoothEvents } from '@/lib/use-booth-events'
import { pageVariants, pageVariantsWide, springPage, springPageWide } from '@/lib/motion'
import { useStore } from '@/store/useStore'

import { routeIndex } from './routes'
import { FrozenOutlet } from './FrozenOutlet'
import { SwipeBackEdge } from './SwipeBackEdge'

/**
 * 모든 화면이 이 껍데기 안에서 돈다.
 *
 * 모바일이 기준이라 기본은 화면을 꽉 채우고, 데스크톱에서는 가운데에 기기 크기의
 * 판을 놓는다. 홈처럼 넓게 쓰는 화면은 판을 넓혀서 좌우로 펼친다.
 */
export function AppShell() {
  const location = useLocation()
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { state: catalog, userId } = useCatalog()
  const index = routeIndex(location.pathname)

  // 서버 찔러보기의 결과를 스토어로 옮긴다. 성사 화면과 거절 토스트가 여기에 달려 있다.
  usePokeSync()

  /**
   * 실제 매칭 알림. 부스 id 가 있어야 구독할 수 있어서, 그 전에는 `useBoothEvents` 가
   * `null` 을 받아 연결하지 않는다.
   *
   * 이미 매칭이나 약속이 화면에 떠 있으면 리듀서가 알아서 무시한다
   * (`server-match-arrived`). 여기서는 파싱과 dispatch 만 한다.
   */
  const boothId = catalog.status === 'ready' ? catalog.boothId : null

  // 실시간으로 받은 제안과 다시 읽어 온 제안을 같은 자리에서 처리한다.
  const applyMatch = (data: unknown) => {
    const match = fromServerMatch(data as ServerMatchSuggested)
    if (!match) {
      console.warn('[match] 주고받을 카드가 비어 있어 알림을 무시합니다', data)
      return
    }
    dispatch({ type: 'server-match-arrived', match })
  }

  useBoothEvents(boothId, userId, {
    /*
      끊겨 있던 동안 온 MATCH_SUGGESTED 는 다시 오지 않는다. 그래서 연결이 붙을 때마다
      대기 중인 제안을 직접 읽는다. 이게 없으면 잠깐 끊긴 사람이 자기에게 온 매칭을
      영영 못 본다.

      제안이 없으면 서버가 204 를 주고, 이미 화면에 떠 있으면 리듀서가 무시한다.
    */
    CONNECTED: () => {
      if (!userId) return
      void (async () => {
        try {
          const pending = await fetchPendingMatch(userId)
          if (pending) applyMatch(pending)
        } catch {
          // 잠깐 실패한 것이면 다음 재연결 때 다시 맞는다.
        }
      })()
    },
    MATCH_SUGGESTED: applyMatch,
    /*
      상대가 거절하거나 약속을 취소하면 지금 보고 있던 화면(매칭 대기, 약속 화면)이
      더 볼 것이 없어진다. 알림 카드를 눌러야만 홈으로 돌아오게 두면 그 화면에 그대로
      갇혀서 "다음" 버튼만 안 눌리는 것처럼 보인다.

      `exchangeId` 를 지금 상태와 대조하고 나서 옮긴다. 이미 다른 매칭·약속으로 넘어간
      뒤에 뒤늦게 도착한 알림이 그 화면을 밀어내면 안 된다 — 리듀서가 같은 이유로
      `server-match-rejected` 를 무시하는 조건과 맞춘다.
    */
    MATCH_REJECTED: (data) => {
      const { exchangeId } = data as { exchangeId: number }
      if (state.match?.exchangeId === exchangeId) navigate('/home')
      dispatch({ type: 'server-match-rejected', exchangeId })
    },
    EXCHANGE_CANCELLED: (data) => {
      const { exchangeId } = data as { exchangeId: number }
      if (state.activeAppointmentId === exchangeId) navigate('/home')
    },
  })

  // 라우트가 바뀐 그 렌더에서 방향을 정해야 첫 프레임부터 올바른 쪽에서 들어온다.
  // 렌더 중 state 를 고치는 것은 이 경우에 React 가 권하는 방식이다.
  // 데스크톱인지에 따라 전환 방식이 다르다. 넓은 화면에서 화면 폭만큼 미는 것은
  // 이동 거리가 너무 길어서 두 화면이 오래 같이 보인다.
  const [wide, setWide] = useState(
    () => typeof window !== 'undefined' && window.matchMedia('(min-width: 768px)').matches,
  )
  useEffect(() => {
    const mq = window.matchMedia('(min-width: 768px)')
    const onChange = () => setWide(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  const [prevIndex, setPrevIndex] = useState(index)
  const [back, setBack] = useState(false)
  if (prevIndex !== index) {
    setBack(index < prevIndex)
    setPrevIndex(index)
  }

  /**
   * 뒤로 갈 기록이 없을 때 navigate(-1) 을 부르면 앱 밖으로 나가서 빈 화면이 뜬다.
   * 새로고침 직후나 주소로 바로 들어온 경우가 그렇다. 그때는 갈 만한 화면으로 보낸다.
   */
  const goBack = () => {
    const idx = (window.history.state as { idx?: number } | null)?.idx ?? 0
    if (idx > 0) navigate(-1)
    else navigate(state.setupDone ? '/home' : '/', { replace: true })
  }
  // 첫 화면과 홈은 뒤로 갈 곳이 없다. 여기서 밀리면 앱이 꺼진 것처럼 보인다.
  const canSwipeBack = location.pathname !== '/' && location.pathname !== '/home'

  /*
    모바일은 화면을 그대로 쓴다. 데스크톱은 폰 모양 판에 가두지 않고 화면 전체를 쓴다.
    판에 담으면 큰 화면에서 UI 만 작게 떠 있어서 배율이 어긋나 보인다.
    홈처럼 넓게 쓰는 화면은 폭을 다 쓰고, 나머지는 가운데 기둥으로 모은다.
  */
  return (
    <div className="h-[100dvh] w-full overflow-hidden bg-white">
      <div className="relative mx-auto flex h-full w-full flex-col overflow-hidden">
        <AnimatePresence custom={back} initial={false}>
          <motion.div
            key={location.pathname}
            custom={back}
            variants={wide ? pageVariantsWide : pageVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={wide ? springPageWide : springPage}
            data-page-pane
            // 노치 바로 밑에 헤더가 붙어 보이지 않게 위쪽 여백을 여기서 한 번에 준다.
            // 배경이 없으면 전환 중에 뒤 화면이 비쳐서 잔상으로 보인다.
            // 판은 항상 화면 전체를 덮는다. 좁은 기둥으로 두면 옆으로 밀어도 화면 밖으로
            // 나가지 않아서 전환 내내 두 판이 나란히 보인다. 내용의 폭은 화면이 각자 정한다.
            className="absolute inset-0 flex flex-col bg-white pt-[max(0.75rem,env(safe-area-inset-top))] md:pt-0"
          >
            <FrozenOutlet />
          </motion.div>
        </AnimatePresence>

        {canSwipeBack && <SwipeBackEdge onBack={goBack} />}

        <Toast message={state.toast} />
      </div>
    </div>
  )
}
