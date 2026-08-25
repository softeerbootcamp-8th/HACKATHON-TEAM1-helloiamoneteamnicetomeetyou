import { motion } from 'motion/react'
import { useNavigate } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { RadarRings } from '@/components/domain/Radar'
import { Button } from '@/components/ui/Button'
import { StatusBar } from '@/components/ui/StatusBar'
import { springPage, springSnap } from '@/lib/motion'
import { ALL_WAITING } from '@/mocks/data'
import { useStore } from '@/store/useStore'

export function Onboarding() {
  const navigate = useNavigate()
  const { dispatch } = useStore()

  const start = () => {
    dispatch({ type: 'onboarded' })
    navigate('/have')
  }

  return (
    <div className="flex h-full flex-col">
      <StatusBar />

      <div className="flex-1 overflow-y-auto px-6 pt-4 no-scrollbar">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.05 }}
        >
          <p className="flex items-center gap-2 text-[15px] font-bold text-ink">
            <span className="size-2 rounded-full bg-brand" />
            현대자동차 팝업
          </p>
          <p className="mt-0.5 pl-4 text-[11px] text-neutral-400">자동차 포토카드 교환</p>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.12 }}
          className="mt-8 text-[30px] leading-[1.32] font-extrabold tracking-[-0.02em] text-ink"
        >
          내 굿즈를 올리면
          <br />
          교환 상대를 찾아드려요
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.19 }}
          className="mt-4 text-[13px] leading-[1.6] text-neutral-400"
        >
          현장에서 쉽고 빠르게
          <br />
          현대자동차 팝업 굿즈를 교환하세요
        </motion.p>

        <div className="relative mt-6 flex h-[280px] items-center justify-center">
          <RadarRings />
          <motion.div
            initial={{ opacity: 0, scale: 0.8, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ ...springSnap, delay: 0.24 }}
          >
            <motion.div
              animate={{ y: [0, -8, 0] }}
              transition={{ duration: 3.6, repeat: Infinity, ease: 'easeInOut' }}
            >
              <CardStack topItemId="sf" count={3} className="w-[150px]" />
            </motion.div>
          </motion.div>
        </div>

        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.34 }}
          className="mx-auto w-fit rounded-full bg-neutral-100 px-4 py-2 text-[12px] font-medium text-neutral-500"
        >
          지금 {ALL_WAITING.length + 6}명이 교환 중이에요
        </motion.p>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button variant="brand" onClick={start}>
          교환하러 가기
        </Button>
      </div>
    </div>
  )
}
