import { AnimatePresence, motion } from 'motion/react'
import { useEffect, type ReactNode } from 'react'

import { cn } from '@/lib/cn'
import { easeOut, springSheet } from '@/lib/motion'

type DialogAction = {
  label: string
  onClick: () => void
}

type Props = {
  open: boolean
  title: string
  description?: string
  /**
   * 위쪽 채워진 버튼. 시안에서 이 자리는 화면마다 뜻이 다르다.
   * 거절하기 모달은 "아니요"(검정), 교환 파토 모달은 "네"(브랜드색)가 여기 온다.
   */
  primary: DialogAction & { tone?: 'ink' | 'brand' }
  /** 아래쪽 흰 버튼 */
  secondary: DialogAction
  /** 바깥을 누르거나 Esc 를 눌렀을 때. 대개 아무것도 하지 않고 닫는 쪽이다. */
  onDismiss: () => void
  children?: ReactNode
}

/**
 * 다이얼로그는 반드시 닫힌다. 바깥 탭, Esc, 두 버튼 셋 다 닫는 길이다.
 * 하나라도 막히면 사용자가 갇힌다.
 */
export function Dialog({
  open,
  title,
  description,
  primary,
  secondary,
  onDismiss,
  children,
}: Props) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onDismiss()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onDismiss])

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="absolute inset-0 z-50 flex items-center justify-center px-8"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={easeOut}
        >
          <button
            type="button"
            aria-label="닫기"
            onClick={onDismiss}
            className="absolute inset-0 bg-black/35"
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            initial={{ opacity: 0, scale: 0.92, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 8 }}
            transition={springSheet}
            className="relative w-full max-w-[300px] rounded-[22px] bg-white p-6 shadow-2xl"
          >
            <h2 className="text-[19px] font-bold text-ink">{title}</h2>
            {description && <p className="mt-2 text-[14px] text-neutral-500">{description}</p>}
            {children}
            <div className="mt-5 space-y-2">
              <motion.button
                type="button"
                whileTap={{ scale: 0.97 }}
                onClick={primary.onClick}
                className={cn(
                  'h-[52px] w-full rounded-full text-[16px] font-bold text-white',
                  primary.tone === 'brand' ? 'bg-brand' : 'bg-ink',
                )}
              >
                {primary.label}
              </motion.button>
              <motion.button
                type="button"
                whileTap={{ scale: 0.97 }}
                onClick={secondary.onClick}
                className="h-[52px] w-full rounded-full border border-neutral-200 bg-white text-[16px] font-bold text-ink"
              >
                {secondary.label}
              </motion.button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
