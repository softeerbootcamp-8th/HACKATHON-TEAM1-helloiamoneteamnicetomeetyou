import { motion } from 'motion/react'
import { useRef, useState } from 'react'

import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import type { Item } from '@/mocks/data'

type Size = 'sm' | 'md' | 'lg'

// 데스크톱은 화면이 넓어서 같은 크기로 두면 카드가 유난히 작아 보인다.
const TILE: Record<Size, string> = {
  sm: 'h-[66px] text-[15px] md:h-[74px] md:text-[16px]',
  md: 'h-[90px] text-[17px] md:h-[100px] md:text-[20px]',
  lg: 'h-[100px] text-[20px] md:h-[112px] md:text-[23px]',
}

/**
 * 카드 앞면. 브랜드색 빛무리 위에 굿즈 이미지를 얹는다.
 *
 * 시안에서 이미지는 회색 타일보다 크게 그려져 위아래 양옆으로 삐져나온다(타일 80x72 에
 * 이미지 96x96). 타일 안에 가두면 차가 작아 보이고 빛무리도 번지지 않아서, 타일만
 * 배경으로 깔고 이미지는 그 위를 덮게 둔다.
 *
 * 이미지를 못 받아오면 약칭 글자가 대신 남는다. 현장 와이파이가 느릴 때 카드가 통째로
 * 비어 보이는 것보다는 낫다.
 */
export function GoodsFace({
  item,
  size = 'md',
  className,
}: {
  item: Item
  size?: Size
  className?: string
}) {
  const [failed, setFailed] = useState(false)

  return (
    <div
      className={cn(
        'relative flex w-full items-center justify-center font-bold text-ink',
        TILE[size],
        className,
      )}
    >
      <span aria-hidden className="absolute inset-0 rounded-xl bg-tile ring-1 ring-line" />
      <span
        aria-hidden
        className="absolute size-[52%] rounded-full blur-[10px]"
        style={{ background: 'radial-gradient(circle, #2cb3edb0 0%, #2cb3ed00 72%)' }}
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
          className="absolute -top-[17%] -left-[10%] h-[133%] w-[120%] max-w-none object-contain select-none"
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
        {/*
          이름 자리를 두 줄로 잡아 둔다. 길이에 따라 늘어나게 두면 'AVANTE N Facelift'
          한 장만 키가 커져서 격자가 어긋난다.
        */}
        <p className="mt-2 flex h-[30px] items-center justify-center text-center text-[12px] leading-tight font-bold text-ink">
          <span className="line-clamp-2">{item.name}</span>
        </p>
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
