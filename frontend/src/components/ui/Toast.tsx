import { AnimatePresence, motion } from 'motion/react'

import { springSheet } from '@/lib/motion'

/** 짧은 알림. 스스로 사라지고 흐름을 막지 않는다. */
export function Toast({ message }: { message: string | null }) {
  return (
    <AnimatePresence>
      {message && (
        <motion.div
          key={message}
          initial={{ opacity: 0, y: 24, scale: 0.96 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 12, scale: 0.98 }}
          transition={springSheet}
          className="pointer-events-none absolute bottom-28 left-1/2 z-50 w-[80%] -translate-x-1/2 rounded-2xl bg-ink/90 px-5 py-3.5 text-center text-[14px] font-semibold text-white backdrop-blur"
        >
          {message}
        </motion.div>
      )}
    </AnimatePresence>
  )
}
