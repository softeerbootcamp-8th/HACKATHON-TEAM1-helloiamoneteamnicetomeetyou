import { AnimatePresence, motion } from 'motion/react'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'

import { Toast } from '@/components/ui/Toast'
import { cn } from '@/lib/cn'
import { pageVariants, springPage } from '@/lib/motion'
import { useStore } from '@/store/useStore'

import { routeIndex, WIDE_ROUTES } from './routes'
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
  // 첫 화면과 홈은 뒤로 갈 곳이 없다. 여기서 밀리면 앱이 꺼진 것처럼 보인다.
  const canSwipeBack = location.pathname !== '/' && location.pathname !== '/home'

  // 데스크톱에서 판이 화면보다 커지면 페이지가 세로로 스크롤된다. 바깥을 화면 높이에
  // 묶고 판은 남는 높이를 채우게 해서, 창을 줄여도 스크롤이 생기지 않게 한다.
  return (
    <div className="flex h-[100dvh] items-center justify-center overflow-hidden bg-neutral-100 md:p-10 lg:p-14">
      <div
        className={cn(
          'relative flex h-full w-full flex-col overflow-hidden bg-white',
          'md:max-h-[880px] md:rounded-[32px] md:shadow-[0_24px_70px_rgba(0,0,0,0.14)]',
          wide ? 'md:max-w-[1080px]' : 'md:max-w-[420px]',
        )}
      >
        <AnimatePresence mode="popLayout" custom={back} initial={false}>
          <motion.div
            key={location.pathname}
            custom={back}
            variants={pageVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={springPage}
            // 노치 바로 밑에 헤더가 붙어 보이지 않게 위쪽 여백을 여기서 한 번에 준다.
            className="absolute inset-0 flex flex-col pt-[max(0.75rem,env(safe-area-inset-top))] md:pt-0"
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>

        {canSwipeBack && <SwipeBackEdge onBack={() => navigate(-1)} />}

        <Toast message={state.toast} />
      </div>
    </div>
  )
}
