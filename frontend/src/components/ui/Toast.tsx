import { AnimatePresence, motion } from 'motion/react'

import { springSheet } from '@/lib/motion'

/**
 * 짧은 알림. 스스로 사라지고 흐름을 막지 않는다.
 * 줄바꿈이 들어 있으면 둘째 줄은 시안처럼 작은 보조 문구로 깐다.
 */
export function Toast({ message }: { message: string | null }) {
  const [head, ...rest] = (message ?? '').split('\n')

  return (
    <AnimatePresence>
      {message && (
        <motion.div
          key={message}
          initial={{ opacity: 0, y: 24, scale: 0.96 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 12, scale: 0.98 }}
          transition={springSheet}
          className="pointer-events-none absolute bottom-28 left-1/2 z-50 w-[80%] max-w-[340px] -translate-x-1/2 rounded-2xl bg-ink/90 px-5 py-3.5 text-center text-white backdrop-blur"
        >
          <p className="text-[14px] font-semibold">{head}</p>
          {rest.length > 0 && <p className="mt-0.5 text-[11px] text-white/70">{rest.join(' ')}</p>}
        </motion.div>
      )}
    </AnimatePresence>
  )
}
