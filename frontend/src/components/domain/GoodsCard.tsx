import { motion } from 'motion/react'
import { useRef } from 'react'

import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import type { Item } from '@/mocks/data'

type Size = 'sm' | 'md' | 'lg'

// 데스크톱은 화면이 넓어서 같은 크기로 두면 카드가 유난히 작아 보인다.
const TILE: Record<Size, string> = {
  sm: 'h-[62px] text-[15px] md:h-[86px] md:text-[19px]',
  md: 'h-[74px] text-[17px] md:h-[88px] md:text-[20px]',
  lg: 'h-[92px] text-[20px] md:h-[104px] md:text-[23px]',
}

/**
 * 카드 앞면. 굿즈 이미지가 아직 없어서 시안처럼 약칭을 초록 빛무리 위에 얹는다.
 * 이미지가 들어오면 이 컴포넌트만 바꾸면 된다.
 */
export function GoodsFace({ item, size = 'md' }: { item: Item; size?: Size }) {
  return (
    <div
      className={cn(
        'relative flex w-full items-center justify-center overflow-hidden rounded-xl bg-tile font-bold text-ink',
        TILE[size],
      )}
    >
      <span
        aria-hidden
        className="absolute size-[70%] rounded-full blur-[14px]"
        style={{ background: 'radial-gradient(circle, #2ced90 0%, #2ced9000 70%)' }}
      />
      <span className="relative">{item.code}</span>
    </div>
  )
}

type CardProps = {
  item: Item
  selected?: boolean
  /** 고를 수 없는 상태. 이유는 note 로 알려 준다. */
  disabled?: boolean
  note?: string
  qty?: number
  showKo?: boolean
  size?: Size
  onClick?: () => void
  onIncrease?: () => void
  onDecrease?: () => void
  /** 꾹 눌러서 선택 취소 */
  onLongPress?: () => void
}

/**
 * 선택 가능한 굿즈 카드. 고른 뒤에는 아래에 수량 스테퍼가 붙는다.
 * 시안에서 Have 는 수량이 있고 Needs 도 같은 모양이라 한 컴포넌트로 쓴다.
 */
export function GoodsCard({
  item,
  selected = false,
  disabled = false,
  note,
  qty,
  showKo = true,
  size = 'md',
  onClick,
  onIncrease,
  onDecrease,
  onLongPress,
}: CardProps) {
  const pressTimer = useRef<number | undefined>(undefined)

  const startPress = () => {
    if (!onLongPress) return
    pressTimer.current = window.setTimeout(onLongPress, 500)
  }
  const endPress = () => {
    if (pressTimer.current) window.clearTimeout(pressTimer.current)
  }

  return (
    <motion.div
      layout
      transition={springSnap}
      className={cn(
        'rounded-2xl bg-white p-2.5 shadow-[0_2px_10px_rgba(0,0,0,0.06)]',
        selected ? 'ring-2 ring-ink' : 'ring-1 ring-neutral-100',
        disabled && 'opacity-45',
      )}
    >
      <motion.button
        type="button"
        onClick={onClick}
        onPointerDown={disabled ? undefined : startPress}
        onPointerUp={endPress}
        onPointerLeave={endPress}
        disabled={disabled}
        whileTap={disabled ? undefined : { scale: 0.95 }}
        transition={springSnap}
        className="w-full text-left disabled:cursor-not-allowed"
        aria-pressed={selected}
        aria-disabled={disabled}
      >
        <div className={cn(disabled && 'grayscale')}>
          <GoodsFace item={item} size={size} />
        </div>
        <p className="mt-2 text-center text-[12px] font-bold text-ink">{item.name}</p>
        {note ? (
          <p className="text-center text-[10px] leading-tight text-neutral-400">{note}</p>
        ) : (
          showKo && <p className="text-center text-[11px] text-neutral-400">{item.nameKo}</p>
        )}
      </motion.button>

      {selected && qty !== undefined && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          transition={springSnap}
          className="mt-1.5 flex items-center justify-between px-1"
        >
          <StepperButton label="하나 빼기" onClick={onDecrease} tone="muted">
            −
          </StepperButton>
          <span className="text-[13px] font-bold text-ink">{qty}</span>
          <StepperButton label="하나 더" onClick={onIncrease} tone="dark">
            ＋
          </StepperButton>
        </motion.div>
      )}
    </motion.div>
  )
}

function StepperButton({
  label,
  onClick,
  tone,
  children,
}: {
  label: string
  onClick?: () => void
  tone: 'muted' | 'dark'
  children: React.ReactNode
}) {
  return (
    <motion.button
      type="button"
      aria-label={label}
      onClick={(e) => {
        e.stopPropagation()
        onClick?.()
      }}
      whileTap={{ scale: 0.85 }}
      transition={springSnap}
      className={cn(
        'flex size-[22px] items-center justify-center rounded-full text-[13px] leading-none font-bold',
        tone === 'dark' ? 'bg-ink text-white' : 'text-neutral-400',
      )}
    >
      {children}
    </motion.button>
  )
}
