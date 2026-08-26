import { motion } from 'motion/react'

import { CARD_SHELL, ItemCardBody } from '@/components/domain/GoodsCard'
import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import { useItem } from '@/features/catalog/useItem'

type Props = {
  /** 맨 위에 보이는 카드. 아직 고른 카드가 없으면 `null` 이고 빈 묶음을 그린다 */
  topItemId: number | null
  /** 뒤에 몇 장이 더 겹쳐 있는지 */
  count: number
  className?: string
  /** 끌고 있는 중이면 살짝 들린다 */
  lifted?: boolean
}

/**
 * 내 카드 묶음. 시안처럼 뒤로 두 장이 어긋나게 겹쳐 있고, 맨 위 카드에만
 * 굿즈 앞면과 이름이 들어간다. 카드 생김새는 다른 화면과 똑같다.
 */
export function CardStack({ topItemId, count, className, lifted = false }: Props) {
  // 고른 카드가 없으면 아무 카드나 세우지 않는다. 가지고 있지도 않은 카드를 내놓을 것처럼
  // 보이면 사용자가 그대로 끌어다 놓는다.
  const item = useItem(topItemId) ?? null
  const behind = item ? Math.min(Math.max(count - 1, 0), 2) : 0

  return (
    <div className={cn('relative w-[112px] md:w-[118px]', className)}>
      {Array.from({ length: behind }).map((_, i) => (
        <motion.div
          key={i}
          aria-hidden
          className={cn(CARD_SHELL, 'absolute inset-0 shadow-[0_4px_14px_rgba(0,0,0,0.08)]')}
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
        className={cn(CARD_SHELL, 'relative p-2.5 shadow-[0_10px_26px_rgba(0,0,0,0.16)]')}
      >
        {item ? (
          <ItemCardBody item={item} size="md" />
        ) : (
          <span className="flex h-[136px] items-center justify-center px-2 text-center text-[12px] leading-[1.6] font-bold text-neutral-400">
            내놓을 카드를
            <br />
            골라보세요
          </span>
        )}
      </motion.div>
    </div>
  )
}
