import { AnimatePresence, motion } from 'motion/react'
import { useEffect, type ReactNode } from 'react'

import { easeOut, springSheet } from '@/lib/motion'

type Props = {
  open: boolean
  title: string
  description?: string
  confirmLabel: string
  cancelLabel: string
  /** 위험한 쪽이 아래 흰 버튼이다. 시안의 "취소할게요" 자리다. */
  onConfirm: () => void
  onCancel: () => void
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
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
  children,
}: Props) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onCancel])

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
            onClick={onCancel}
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
                onClick={onCancel}
                className="h-[52px] w-full rounded-full bg-ink text-[16px] font-bold text-white"
              >
                {cancelLabel}
              </motion.button>
              <motion.button
                type="button"
                whileTap={{ scale: 0.97 }}
                onClick={onConfirm}
                className="h-[52px] w-full rounded-full border border-neutral-200 bg-white text-[16px] font-bold text-ink"
              >
                {confirmLabel}
              </motion.button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
