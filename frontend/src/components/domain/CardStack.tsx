import { motion } from 'motion/react'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import { itemById } from '@/mocks/data'

type Props = {
  /** 맨 위에 보이는 카드 */
  topItemId: string
  /** 뒤에 몇 장이 더 겹쳐 있는지 */
  count: number
  className?: string
  /** 끌고 있는 중이면 살짝 들린다 */
  lifted?: boolean
}

/**
 * 내 카드 묶음. 시안처럼 뒤로 두 장이 어긋나게 겹쳐 있고, 맨 위 카드에만
 * 굿즈 앞면과 이름이 들어간다.
 */
export function CardStack({ topItemId, count, className, lifted = false }: Props) {
  const item = itemById(topItemId)
  const behind = Math.min(Math.max(count - 1, 0), 2)

  return (
    <div className={cn('relative aspect-[112/140] w-[112px] md:w-[118px]', className)}>
      {Array.from({ length: behind }).map((_, i) => (
        <motion.div
          key={i}
          aria-hidden
          className="absolute inset-0 rounded-2xl bg-white shadow-[0_4px_14px_rgba(0,0,0,0.08)]"
          animate={{
            rotate: -(i + 1) * 5,
            x: -(i + 1) * 5,
            y: (i + 1) * 2,
            scale: 1 - (i + 1) * 0.02,
          }}
          transition={springSnap}
        />
      ))}
      <motion.div
        animate={{ rotate: lifted ? -3 : -6, scale: lifted ? 1.06 : 1 }}
        transition={springSnap}
        className="absolute inset-0 rounded-2xl bg-white p-2.5 shadow-[0_10px_26px_rgba(0,0,0,0.16)]"
      >
        <GoodsFace item={item} size="md" />
        <p className="mt-1.5 text-center text-[12px] leading-tight font-bold tracking-tight text-ink md:text-[13px]">
          {item.name}
        </p>
      </motion.div>
    </div>
  )
}
