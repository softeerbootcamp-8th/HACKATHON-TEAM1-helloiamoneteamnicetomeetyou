import { motion } from 'motion/react'
import { useRef, useState } from 'react'

import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import type { Item } from '@/mocks/data'

type Size = 'sm' | 'md' | 'lg'

// 데스크톱은 화면이 넓어서 같은 크기로 두면 카드가 유난히 작아 보인다.
const TILE: Record<Size, string> = {
  sm: 'h-[62px] text-[15px] md:h-[70px] md:text-[16px]',
  md: 'h-[74px] text-[17px] md:h-[88px] md:text-[20px]',
  lg: 'h-[92px] text-[20px] md:h-[104px] md:text-[23px]',
}

/**
 * 카드 앞면. 굿즈 이미지를 브랜드색 빛무리 위에 얹는다.
 *
 * 이미지를 못 받아오면 약칭 글자가 대신 남는다. 현장 와이파이가 느릴 때 카드가 통째로
 * 비어 보이는 것보다는 낫다.
 */
export function GoodsFace({ item, size = 'md' }: { item: Item; size?: Size }) {
  const [failed, setFailed] = useState(false)

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
        style={{ background: 'radial-gradient(circle, #2cb3ed 0%, #2cb3ed00 70%)' }}
      />
      {failed ? (
        <span className="relative">{item.code}</span>
      ) : (
        <img
          src={item.image}
          alt=""
          aria-hidden
          loading="lazy"
          draggable={false}
          onError={() => setFailed(true)}
          className="relative size-full object-contain p-1.5 select-none"
        />
      )}
    </div>
  )
}

type CardProps = {
  item: Item
  selected?: boolean
  /** 고를 수 없는 상태. 시안에서는 이유를 글로 적지 않고 흐리게만 둔다. */
  disabled?: boolean
  qty?: number
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
 *
 * 카드 안에는 굵은 아이템 이름 한 줄만 둔다. 한글 이름 같은 서브 텍스트는
 * 시안에서 전부 빠졌다.
 */
export function GoodsCard({
  item,
  selected = false,
  disabled = false,
  qty,
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
        <p className="mt-2 text-center text-[12px] leading-tight font-bold text-ink">{item.name}</p>
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
