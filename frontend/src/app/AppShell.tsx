import { AnimatePresence, motion } from 'motion/react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'

import { Toast } from '@/components/ui/Toast'
import { cn } from '@/lib/cn'
import { pageVariants, springPage } from '@/lib/motion'
import { useStore } from '@/store/useStore'

import { routeIndex, WIDE_ROUTES } from './routes'
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
  const { state } = useStore()
  const index = routeIndex(location.pathname)

  // 라우트가 바뀐 그 렌더에서 방향을 정해야 첫 프레임부터 올바른 쪽에서 들어온다.
  // 렌더 중 state 를 고치는 것은 이 경우에 React 가 권하는 방식이다.
  const [prevIndex, setPrevIndex] = useState(index)
  const [back, setBack] = useState(false)
  if (prevIndex !== index) {
    setBack(index < prevIndex)
    setPrevIndex(index)
  }

  const wide = WIDE_ROUTES.has(location.pathname)

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
            variants={pageVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={springPage}
            data-page-pane
            // 노치 바로 밑에 헤더가 붙어 보이지 않게 위쪽 여백을 여기서 한 번에 준다.
            // 배경이 없으면 전환 중에 뒤 화면이 비쳐서 잔상으로 보인다.
            className={cn(
              'absolute inset-0 flex flex-col bg-white pt-[max(0.75rem,env(safe-area-inset-top))] md:pt-0',
              // 넓게 쓰는 화면이 아니면 데스크톱에서 가운데 기둥으로 모은다.
              !wide && 'md:mx-auto md:w-full md:max-w-[560px]',
            )}
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
