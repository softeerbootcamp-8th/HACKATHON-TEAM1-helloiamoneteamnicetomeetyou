import { motion } from 'motion/react'
import type { ReactNode } from 'react'

import { Button } from '@/components/ui/Button'
import { SparkleIcon } from '@/components/ui/icons'
import { springSnap } from '@/lib/motion'

/**
 * 보여줄 것이 없을 때 쓰는 화면. 시안에는 없는 자리라 다른 화면의 규칙을 그대로 따른다.
 * 브랜드색 빛무리에 아이콘을 얹고, 굵은 한 줄과 회색 설명, 아래에 알약 버튼이다.
 */
export function EmptyState({
  title,
  description,
  icon,
  actionLabel = '교환 대기존으로',
  onAction,
}: {
  title: string
  description?: string
  icon?: ReactNode
  actionLabel?: string
  /** 없으면 버튼 자리를 통째로 뺀다. 기다리는 중이라 누를 것이 없는 화면이 쓴다. */
  onAction?: () => void
}) {
  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex flex-1 flex-col items-center justify-center px-8 text-center">
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={springSnap}
          className="anim-float-sm relative mb-8 flex size-[104px] items-center justify-center"
        >
          <span aria-hidden className="absolute inset-0 rounded-full bg-brand/10" />
          <span
            aria-hidden
            className="absolute size-[64%] rounded-full blur-[16px]"
            style={{ background: 'radial-gradient(circle, #2cb3edb0 0%, #2cb3ed00 74%)' }}
          />
          <span className="relative text-brand">{icon ?? <SparkleIcon className="size-9" />}</span>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springSnap, delay: 0.06 }}
          className="text-[20px] font-extrabold tracking-[-0.02em] text-ink"
        >
          {title}
        </motion.h1>
        {description && (
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.14 }}
            className="mt-2.5 text-[13px] leading-[1.6] text-neutral-400"
          >
            {description}
          </motion.p>
        )}
      </div>

      {onAction && (
        <div className="shrink-0 px-6 pt-4 pb-8">
          <Button variant="brand" onClick={onAction}>
            {actionLabel}
          </Button>
        </div>
      )}
    </div>
  )
}
