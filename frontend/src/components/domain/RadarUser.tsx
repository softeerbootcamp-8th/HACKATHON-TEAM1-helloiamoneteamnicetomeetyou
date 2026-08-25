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
  /** 방금 이 사람에게 카드를 놓았는지. 고리가 한 번 터진다. */
  burst?: boolean
  /** 카드를 눌렀을 때. 끌어놓기 말고 그냥 눌러서도 찔러볼 수 있어야 한다. */
  onSelect?: () => void
  index: number
}

/** 레이더 위에 서 있는 상대 한 명. */
export function RadarUser({ user, hovered, pending, burst = false, onSelect, index }: Props) {
  const item = itemById(user.itemId)

  return (
    <motion.div
      data-radar-user={user.id}
      initial={{ opacity: 0, scale: 0.6 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ ...springSnap, delay: 0.08 * index }}
      className="relative w-[76px]"
    >
      {burst && (
        <span aria-hidden className="pointer-events-none absolute inset-0 z-20">
          {/* 고리 두 개를 살짝 어긋나게 띄워서 물결처럼 퍼지게 한다. */}
          {[0, 0.28].map((delay) => (
            <span
              key={delay}
              className="anim-ripple absolute top-1/2 left-1/2 aspect-square w-full rounded-full border-[3px] border-brand"
              style={{ animationDelay: `${delay}s` }}
            />
          ))}
        </span>
      )}

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

      {/* 떠다니는 것은 CSS 가 맡고, 손가락이 올라왔을 때의 확대만 motion 이 맡는다.
          둘을 한 요소에 겹치면 transform 이 서로를 덮어써서 하나가 죽는다. */}
      <motion.button
        type="button"
        onClick={onSelect}
        disabled={pending || !onSelect}
        aria-label={`${user.nickname}님에게 찔러보기`}
        whileTap={pending ? undefined : { scale: 0.94 }}
        animate={{ scale: hovered ? 1.08 : 1 }}
        transition={springSnap}
        style={{ animationDelay: `${index * 0.4}s` }}
        className={cn(
          'block w-full rounded-2xl bg-white p-2 shadow-[0_4px_16px_rgba(0,0,0,0.10)]',
          !hovered && !pending && 'anim-float-sm',
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
      </motion.button>
    </motion.div>
  )
}
