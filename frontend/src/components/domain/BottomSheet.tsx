import { motion, useDragControls, useMotionValue, type PanInfo } from 'motion/react'
import type { ReactNode } from 'react'

import { springSheet, springSnap } from '@/lib/motion'

type Props = {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** 접혀 있을 때 보이는 머리 부분 높이 */
  peek: number
  /** 펼쳤을 때의 전체 높이 */
  height: number
  header: ReactNode
  children: ReactNode
}

/**
 * 위로 스와이프하면 펼쳐지는 바텀시트. 머리를 누르는 것으로도 열리고 닫힌다.
 * 끄는 속도를 그대로 이어받아야 손에 붙은 느낌이 난다.
 */
export function BottomSheet({ open, onOpenChange, peek, height, header, children }: Props) {
  const collapsedY = height - peek
  const y = useMotionValue(open ? 0 : collapsedY)
  const dragControls = useDragControls()

  const handleDragEnd = (_: unknown, info: PanInfo) => {
    // 빠르게 튕기면 방향만 보고, 천천히 끌면 절반을 넘었는지로 정한다.
    const fast = Math.abs(info.velocity.y) > 380
    const shouldOpen = fast ? info.velocity.y < 0 : y.get() < collapsedY / 2
    onOpenChange(shouldOpen)
  }

  return (
    <motion.div
      drag="y"
      dragControls={dragControls}
      dragListener={false}
      dragConstraints={{ top: 0, bottom: collapsedY }}
      dragElastic={0.04}
      dragMomentum={false}
      style={{ y, height }}
      animate={{ y: open ? 0 : collapsedY }}
      transition={springSheet}
      onDragEnd={handleDragEnd}
      className="absolute inset-x-0 bottom-0 z-30 flex flex-col rounded-t-[26px] bg-white shadow-[0_-8px_30px_rgba(0,0,0,0.10)]"
    >
      {/* touch-action 을 시트 전체에 주면 안쪽 목록이 손가락으로 안 굴러간다.
          끄는 것은 손잡이에서만 시작하게 하고, 아래 목록은 그대로 스크롤되게 둔다. */}
      <motion.button
        type="button"
        onClick={() => onOpenChange(!open)}
        aria-expanded={open}
        whileTap={{ scale: 0.99 }}
        transition={springSnap}
        onPointerDown={(e) => dragControls.start(e)}
        className="shrink-0 touch-none px-5 pt-2.5 pb-3"
      >
        <span aria-hidden className="mx-auto block h-1 w-9 rounded-full bg-neutral-200" />
        <div className="mt-3">{header}</div>
      </motion.button>

      <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-6 no-scrollbar">{children}</div>
    </motion.div>
  )
}
