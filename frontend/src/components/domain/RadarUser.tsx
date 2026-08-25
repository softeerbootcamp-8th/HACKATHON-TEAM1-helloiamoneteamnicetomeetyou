import { motion } from 'motion/react'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import { itemById, type WaitingUser } from '@/mocks/data'

type Props = {
  user: WaitingUser
  /** 지금 내 카드 묶음이 이 사람 위에 올라와 있는지 */
  hovered: boolean
  /** 이미 찔러보기를 보내고 답을 기다리는 중인지 */
  pending: boolean
  index: number
}

/** 레이더 위에 서 있는 상대 한 명. */
export function RadarUser({ user, hovered, pending, index }: Props) {
  const item = itemById(user.itemId)

  return (
    <motion.div
      data-radar-user={user.id}
      initial={{ opacity: 0, scale: 0.6 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ ...springSnap, delay: 0.08 * index }}
      className="relative w-[76px]"
    >
      {hovered && (
        <motion.span
          initial={{ opacity: 0, y: 6, scale: 0.8 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={springSnap}
          className="absolute -top-8 left-1/2 z-10 -translate-x-1/2 rounded-lg bg-ink px-2.5 py-1 text-[11px] font-bold whitespace-nowrap text-white"
        >
          찔러보기
        </motion.span>
      )}

      <motion.div
        animate={{
          scale: hovered ? 1.08 : 1,
          y: pending ? 0 : [0, -3, 0],
        }}
        transition={
          hovered ? springSnap : { duration: 3 + index * 0.4, repeat: Infinity, ease: 'easeInOut' }
        }
        className={cn(
          'rounded-2xl bg-white p-2 shadow-[0_4px_16px_rgba(0,0,0,0.10)]',
          hovered && 'ring-2 ring-ink',
          pending && 'opacity-45 grayscale',
        )}
      >
        <GoodsFace item={item} size="sm" />
        <p className="mt-1.5 text-center text-[10px] font-bold text-ink">{item.name}</p>
        <p className="text-center text-[9px] text-neutral-400">{user.nickname}</p>

        {pending && (
          <span className="absolute inset-0 flex items-center justify-center text-[15px] font-bold text-neutral-500">
            ···
          </span>
        )}
        {hovered && (
          <span className="absolute -top-1.5 -right-1.5 flex size-5 items-center justify-center rounded-full bg-ink text-[10px] text-white">
            ✓
          </span>
        )}
      </motion.div>
    </motion.div>
  )
}
