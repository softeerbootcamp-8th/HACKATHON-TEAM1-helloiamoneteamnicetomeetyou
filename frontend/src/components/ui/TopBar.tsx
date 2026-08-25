import { motion } from 'motion/react'
import type { ReactNode } from 'react'

import { springSnap } from '@/lib/motion'

type Props = {
  title?: string
  onBack?: () => void
  onClose?: () => void
  right?: ReactNode
}

/** 시안의 상단 바. 뒤로가기와 닫기가 각각 다른 화면에 나온다. */
export function TopBar({ title, onBack, onClose, right }: Props) {
  return (
    <div className="relative flex h-14 shrink-0 items-center px-4">
      {onBack && (
        <motion.button
          type="button"
          aria-label="뒤로"
          onClick={onBack}
          whileTap={{ scale: 0.88 }}
          transition={springSnap}
          className="-ml-2 flex size-10 items-center justify-center text-2xl text-ink"
        >
          ‹
        </motion.button>
      )}
      {title && (
        <h1 className="pointer-events-none absolute left-1/2 -translate-x-1/2 text-[17px] font-bold text-ink">
          {title}
        </h1>
      )}
      <div className="ml-auto flex items-center gap-1">
        {right}
        {onClose && (
          <motion.button
            type="button"
            aria-label="닫기"
            onClick={onClose}
            whileTap={{ scale: 0.88 }}
            transition={springSnap}
            className="flex size-10 items-center justify-center text-[26px] font-light text-ink"
          >
            ✕
          </motion.button>
        )}
      </div>
    </div>
  )
}
