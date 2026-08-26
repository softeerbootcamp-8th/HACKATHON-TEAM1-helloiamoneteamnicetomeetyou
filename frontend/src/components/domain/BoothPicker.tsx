import { AnimatePresence, motion } from 'motion/react'
import { useEffect } from 'react'

import type { ServerBooth } from '@/features/catalog/api'
import { cn } from '@/lib/cn'
import { easeOut, springSheet } from '@/lib/motion'

type Props = {
  open: boolean
  booths: ServerBooth[]
  selectedId: number | null
  onSelect: (boothId: number) => void
  onDismiss: () => void
}

/**
 * 붙을 부스를 고르는 모달.
 *
 * `ui/Dialog` 를 쓰지 않는다. 그쪽은 버튼 두 개로 답하는 확인 모달이라 목록이 들어갈 자리가
 * 없다. 대신 배경과 모서리, 스프링은 같은 값을 써서 같은 앱으로 보이게 맞춰 둔다.
 *
 * 고르면 바로 닫힌다. "선택" 버튼을 한 번 더 누르게 하면 시연 중에 손이 하나 더 든다.
 */
export function BoothPicker({ open, booths, selectedId, onSelect, onDismiss }: Props) {
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
            aria-label="부스 고르기"
            initial={{ opacity: 0, scale: 0.92, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 8 }}
            transition={springSheet}
            className="relative flex max-h-[70%] w-full max-w-[300px] flex-col rounded-[22px] bg-white p-6 shadow-2xl"
          >
            <h2 className="shrink-0 text-[19px] font-bold text-ink">어느 부스인가요?</h2>
            <p className="mt-2 shrink-0 text-[14px] text-neutral-500">
              고른 부스의 굿즈로 교환 상대를 찾아요.
            </p>

            {/* 부스가 많아지면 여기만 굴러간다. 모달 전체가 화면을 넘어가면 안 된다. */}
            <div className="mt-4 min-h-0 flex-1 space-y-2 overflow-y-auto no-scrollbar">
              {booths.map((booth) => {
                const current = booth.id === selectedId
                return (
                  <motion.button
                    key={booth.id}
                    type="button"
                    whileTap={{ scale: 0.97 }}
                    aria-current={current}
                    onClick={() => onSelect(booth.id)}
                    className={cn(
                      'flex w-full items-center gap-2.5 rounded-2xl border px-4 py-3 text-left',
                      current ? 'border-ink bg-neutral-50' : 'border-neutral-200 bg-white',
                    )}
                  >
                    <span
                      aria-hidden
                      className={cn(
                        'size-2 shrink-0 rounded-full',
                        current ? 'bg-brand' : 'bg-neutral-200',
                      )}
                    />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[15px] font-bold text-ink">
                        {booth.name}
                      </span>
                      {booth.description && (
                        <span className="mt-0.5 block truncate text-[11px] text-neutral-400">
                          {booth.description}
                        </span>
                      )}
                    </span>
                  </motion.button>
                )
              })}
            </div>

            <motion.button
              type="button"
              whileTap={{ scale: 0.97 }}
              onClick={onDismiss}
              className="mt-5 h-[52px] w-full shrink-0 rounded-full border border-neutral-200 bg-white text-[16px] font-bold text-ink"
            >
              닫기
            </motion.button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
