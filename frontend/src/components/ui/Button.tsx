import { motion, type HTMLMotionProps } from 'motion/react'
import type { ReactNode } from 'react'

import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'

type Variant = 'primary' | 'brand' | 'ghost' | 'outline'

// React 의 onAnimationStart 와 motion 의 것이 서로 달라서, 네이티브 button 속성을
// 그대로 펼치면 타입이 부딪힌다. motion 쪽 타입을 바탕으로 쓴다.
type Props = {
  variant?: Variant
  children: ReactNode
} & Omit<HTMLMotionProps<'button'>, 'ref' | 'children'>

const STYLES: Record<Variant, string> = {
  // 시안의 검정 알약 버튼
  primary: 'bg-ink text-white',
  // 시안의 초록 알약 버튼. 확정처럼 기분 좋은 자리에만 쓴다. 글자는 흰색이다.
  brand: 'bg-brand text-white',
  ghost: 'bg-transparent text-neutral-600',
  outline: 'bg-white text-ink border border-neutral-200',
}

/**
 * 눌리는 느낌이 반드시 있어야 한다. 모바일에는 hover 가 없어서 press 축소가
 * 사용자가 받는 유일한 피드백이다.
 */
export function Button({ variant = 'primary', className, children, disabled, ...rest }: Props) {
  return (
    <motion.button
      type="button"
      disabled={disabled}
      whileTap={disabled ? undefined : { scale: 0.97 }}
      transition={springSnap}
      className={cn(
        'h-[54px] w-full rounded-full text-[16px] font-bold',
        'disabled:pointer-events-none disabled:bg-disabled disabled:text-neutral-400',
        STYLES[variant],
        className,
      )}
      {...rest}
    >
      {children}
    </motion.button>
  )
}

/** CTA 아래에 붙는 보조 동작. 시안에서 "취소", "홈으로" 자리다. */
export function TextButton({ className, children, ...rest }: Omit<Props, 'variant'>) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.95 }}
      transition={springSnap}
      className={cn('mx-auto block py-3 text-[14px] font-semibold text-neutral-500', className)}
      {...rest}
    >
      {children}
    </motion.button>
  )
}
