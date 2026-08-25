import { AnimatePresence, motion } from 'motion/react'
import { useState } from 'react'
import { Outlet, useLocation } from 'react-router'

import { Toast } from '@/components/ui/Toast'
import { cn } from '@/lib/cn'
import { pageVariants, springPage } from '@/lib/motion'
import { useStore } from '@/store/useStore'

import { routeIndex, WIDE_ROUTES } from './routes'

/**
 * 모든 화면이 이 껍데기 안에서 돈다.
 *
 * 모바일이 기준이라 기본은 화면을 꽉 채우고, 데스크톱에서는 가운데에 기기 크기의
 * 판을 놓는다. 홈처럼 넓게 쓰는 화면은 판을 넓혀서 좌우로 펼친다.
 */
export function AppShell() {
  const location = useLocation()
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

  return (
    <div className="flex min-h-full items-center justify-center bg-neutral-100 md:p-6">
      <div
        className={cn(
          'relative flex h-[100dvh] w-full flex-col overflow-hidden bg-white',
          'md:h-[860px] md:rounded-[32px] md:shadow-[0_24px_70px_rgba(0,0,0,0.14)]',
          wide ? 'md:max-w-[1040px]' : 'md:max-w-[420px]',
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
            className="absolute inset-0 flex flex-col"
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>

        <Toast message={state.toast} />
      </div>
    </div>
  )
}
