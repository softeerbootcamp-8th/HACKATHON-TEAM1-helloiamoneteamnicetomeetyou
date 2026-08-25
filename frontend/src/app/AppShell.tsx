import { AnimatePresence, motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'

import { Toast } from '@/components/ui/Toast'
import { useCatalog } from '@/features/catalog/useCatalog'
import { fromServerMatch, type ServerMatchSuggested } from '@/features/matching/from-server-match'
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

  /**
   * 실제 매칭 알림. 카탈로그가 준비돼야(부스 id 와 카드 이름 매핑이 있어야) 구독하고
   * 화면에 그릴 수 있어서, 그 전에는 `useBoothEvents` 가 `null` 을 받아 연결하지 않는다.
   *
   * 이미 매칭이나 약속이 화면에 떠 있으면 리듀서가 알아서 무시한다
   * (`server-match-arrived`). 여기서는 파싱과 dispatch 만 한다.
   */
  const boothId = catalog.status === 'ready' ? catalog.boothId : null
  useBoothEvents(boothId, userId, {
    MATCH_SUGGESTED: (data) => {
      if (catalog.status !== 'ready') return
      const match = fromServerMatch(data as ServerMatchSuggested, catalog.mockIdOf)
      if (!match) {
        console.warn('[match] 카드 매핑을 찾지 못해 알림을 무시합니다', data)
        return
      }
      dispatch({ type: 'server-match-arrived', match })
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
