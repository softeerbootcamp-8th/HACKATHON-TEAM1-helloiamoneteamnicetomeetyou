import { AnimatePresence, motion } from 'motion/react'
import { useEffect } from 'react'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { cn } from '@/lib/cn'
import { easeOut, springSheet, springSnap, staggerChild, staggerParent } from '@/lib/motion'
import { itemById } from '@/mocks/data'
import type { Selection } from '@/store/types'

type Props = {
  open: boolean
  /** 내가 내놓기로 한 카드와 개수 */
  have: Selection[]
  onClose: () => void
  onEdit: () => void
}

/**
 * 내 카드 묶음을 펼쳐 본다. 시안 `4_v1`(3장 미만) / `4_v2`(3장 이상) 두 상태다.
 *
 * <p>3장이 넘으면 카드를 나란히 늘리지 않고 <b>같은 카드를 겹쳐 쌓는다</b>(시안 225:24755).
 * 열 개를 등록한 사람도 한 화면에 들어와야 하고, 개수는 겹친 장수보다 숫자로 읽는 편이 빠르다.
 */
export function MyCardsSheet({ open, have, onClose, onEdit }: Props) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  const total = have.reduce((sum, s) => sum + s.qty, 0)
  // 시안이 3장을 경계로 두 화면을 나눠 뒀다.
  const stacked = have.length >= 3

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="absolute inset-0 z-40 flex flex-col justify-end"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={easeOut}
        >
          <button
            type="button"
            aria-label="닫기"
            onClick={onClose}
            className="absolute inset-0 bg-black/35"
          />

          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label="내 카드 묶음"
            initial={{ y: 32, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: 24, opacity: 0 }}
            transition={springSheet}
            className="relative max-h-[76%] overflow-y-auto rounded-t-[26px] bg-white px-6 pt-5 pb-8 no-scrollbar"
          >
            <span aria-hidden className="mx-auto block h-1 w-9 rounded-full bg-neutral-200" />

            <div className="mt-4 flex items-baseline justify-between">
              <h2 className="text-[18px] font-extrabold tracking-[-0.02em] text-ink">내 카드</h2>
              <span className="text-[12px] text-neutral-400">{total}장</span>
            </div>

            {have.length === 0 ? (
              <p className="py-8 text-center text-[13px] text-neutral-400">
                아직 내놓을 카드를 고르지 않았어요.
              </p>
            ) : (
              <motion.ul
                variants={staggerParent}
                initial="hidden"
                animate="show"
                className={cn('mt-4 grid gap-3', stacked ? 'grid-cols-3' : 'grid-cols-2')}
              >
                {have.map((sel) => (
                  <motion.li key={sel.itemId} variants={staggerChild}>
                    <CardPile itemId={sel.itemId} qty={sel.qty} stacked={stacked} />
                  </motion.li>
                ))}
              </motion.ul>
            )}

            <motion.button
              type="button"
              onClick={onEdit}
              whileTap={{ scale: 0.97 }}
              transition={springSnap}
              className="mt-6 h-[52px] w-full rounded-full bg-ink text-[16px] font-bold text-white"
            >
              편집하기
            </motion.button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

/** 카드 한 종류. 여러 장이면 뒤로 겹쳐 쌓고 개수를 숫자로 붙인다. */
function CardPile({ itemId, qty, stacked }: { itemId: string; qty: number; stacked: boolean }) {
  const item = itemById(itemId)
  // 겹치는 장수는 두 장까지만 보여 준다. 그 이상은 그림이 지저분해지고 숫자로 이미 읽힌다.
  const behind = stacked ? Math.min(Math.max(qty - 1, 0), 2) : 0

  return (
    <div className="relative">
      {Array.from({ length: behind }).map((_, i) => (
        <span
          key={i}
          aria-hidden
          className="absolute inset-0 rounded-2xl bg-white shadow-[0_2px_10px_rgba(0,0,0,0.08)]"
          style={{ transform: `rotate(${-(i + 1) * 4}deg) translateX(${-(i + 1) * 4}px)` }}
        />
      ))}

      <div className="relative rounded-2xl bg-white p-2.5 ring-1 ring-neutral-100">
        <GoodsFace item={item} size={stacked ? 'sm' : 'md'} />
        <p className="mt-2 truncate text-center text-[12px] font-bold text-ink">{item.name}</p>
        {qty > 1 && (
          <span className="absolute -top-1.5 -right-1.5 flex size-[22px] items-center justify-center rounded-full bg-ink text-[11px] font-bold text-white">
            {qty}
          </span>
        )}
      </div>
    </div>
  )
}
