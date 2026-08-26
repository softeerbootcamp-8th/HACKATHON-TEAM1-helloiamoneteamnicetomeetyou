import { motion } from 'motion/react'
import { useRef, useState, type ReactNode } from 'react'

import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import type { Item } from '@/features/catalog/api'

export type Size = 'sm' | 'md' | 'lg' | 'fill'

/**
 * 카드 앞면(회색 타일)의 높이와, 이미지를 못 받았을 때 대신 나오는 약칭 글자 크기다.
 * 데스크톱은 화면이 넓어서 같은 크기로 두면 카드가 유난히 작아 보인다.
 */
const TILE: Record<Size, string> = {
  sm: 'h-[70px] text-[15px] md:h-[78px] md:text-[16px]',
  md: 'h-[96px] text-[17px] md:h-[106px] md:text-[20px]',
  lg: 'h-[108px] text-[20px] md:h-[118px] md:text-[23px]',
  fill: 'h-full text-[22px]',
}

/** 이름 자리. 두 줄까지 들어가게 높이를 잡아 둬야 격자가 어긋나지 않는다. */
const NAME: Record<Size, string> = {
  sm: 'h-[26px] text-[10px]',
  md: 'h-[30px] text-[12px]',
  lg: 'h-[32px] text-[13px]',
  fill: 'h-[32px] text-[13px]',
}

const PAD: Record<Size, string> = {
  sm: 'p-2',
  md: 'p-2.5',
  lg: 'p-3',
  fill: 'p-3',
}

/**
 * 카드 겉모양. 굿즈 카드는 화면마다 쓰임이 달라도 생김새는 하나여야 한다.
 * 흰 바닥에 옅은 테두리, 시안의 `#e8e8e5` 자리다.
 */
export const CARD_SHELL = 'rounded-2xl bg-white ring-1 ring-line'

/**
 * 강조된 카드의 겉모양. 고른 카드와 끌어다 올린 표적이 이걸 쓴다.
 *
 * <b>`CARD_SHELL` 위에 `ring-2 ring-ink` 를 덧붙이는 방식으로는 색이 바뀌지 않는다.</b>
 * `cn` 은 겹치는 클래스를 정리하지 않아서 `ring-line` 과 `ring-ink` 가 둘 다 남고, 생성된
 * CSS 에서 `.ring-line` 이 뒤에 있어서(테마에 `--color-line` 이 `--color-ink` 보다 뒤에
 * 적혀 있다) 옅은 테두리가 이긴다. 그래서 두 겹으로 얹지 않고 처음부터 하나만 준다.
 */
export const CARD_SHELL_ACTIVE = 'rounded-2xl bg-white ring-2 ring-ink'

/**
 * 카드 앞면. 브랜드색 빛무리 위에 굿즈 이미지를 얹는다.
 *
 * 시안에서 이미지는 회색 타일보다 크게 그려져 위아래 양옆으로 삐져나온다. 타일 안에
 * 가두면 차가 작아 보이고 빛무리도 번지지 않아서, 타일만 배경으로 깔고 이미지는 그 위를
 * 덮게 둔다. 다만 카드 여백보다 더 나가면 카드를 벗어난 것처럼 보여서 118% 로 맞췄다.
 *
 * `max-w-none` 이 없으면 preflight 의 `img { max-width: 100% }` 가 폭을 잘라서
 * 이미지가 넓어지지 않고 왼쪽으로만 밀린다.
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

  // 주소가 아예 없는 카드도 있다. 어드민에서 그림을 아직 안 넣은 경우인데, 빈 src 로 img 를
  // 걸면 브라우저가 현재 페이지를 다시 받으러 가므로 처음부터 약칭으로 그린다.
  const showCode = failed || !item.imageUrl

  return (
    <div
      className={cn(
        'relative flex w-full shrink-0 items-center justify-center font-bold text-ink',
        TILE[size],
        className,
      )}
    >
      <span aria-hidden className="absolute inset-0 rounded-xl bg-tile ring-1 ring-line" />
      <span
        aria-hidden
        className="absolute size-[58%] rounded-full blur-[11px]"
        style={{ background: 'radial-gradient(circle, #2cb3edd9 0%, #2cb3ed00 74%)' }}
      />
      {showCode ? (
        <span className="relative">{item.code}</span>
      ) : (
        <img
          src={item.imageUrl}
          alt=""
          aria-hidden
          loading="lazy"
          draggable={false}
          /*
            서비스 워커가 이 버킷을 캐시한다(`sw.ts`). crossOrigin 이 없으면 응답이 opaque 로
            와서 성공·실패를 가릴 수 없어 캐시에 못 넣는다. 버킷이 CORS 를 열어 두어서 붙인다.
          */
          crossOrigin="anonymous"
          onError={() => setFailed(true)}
          className="absolute -top-[9%] -left-[9%] h-[118%] w-[118%] max-w-none object-contain select-none"
        />
      )}
    </div>
  )
}

/** 카드 속. 앞면과 이름만 들어간다. 모든 화면이 이걸 그대로 쓴다. */
export function ItemCardBody({ item, size = 'md' }: { item: Item; size?: Size }) {
  return (
    <>
      <GoodsFace item={item} size={size} />
      <p
        className={cn(
          'mt-2 flex items-center justify-center text-center leading-tight font-bold text-ink',
          NAME[size],
        )}
      >
        <span className="line-clamp-2">{item.name}</span>
      </p>
    </>
  )
}

/** 누를 일이 없는 자리에 쓰는 카드. 매칭 결과, 찔러보기 확인 같은 화면이 쓴다. */
export function ItemCard({
  item,
  size = 'md',
  className,
  children,
}: {
  item: Item
  size?: Size
  className?: string
  children?: ReactNode
}) {
  return (
    <div
      className={cn(
        CARD_SHELL,
        PAD[size],
        'shadow-[0_4px_16px_rgba(0,0,0,0.08)]',
        'relative',
        className,
      )}
    >
      <ItemCardBody item={item} size={size} />
      {children}
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
        selected ? CARD_SHELL_ACTIVE : CARD_SHELL,
        PAD[size],
        'shadow-[0_2px_10px_rgba(0,0,0,0.06)]',
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
          <ItemCardBody item={item} size={size} />
        </div>
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
