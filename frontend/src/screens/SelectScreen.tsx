import { motion } from 'motion/react'

import { GoodsCard } from '@/components/domain/GoodsCard'
import { Button } from '@/components/ui/Button'
import { TopBar } from '@/components/ui/TopBar'
import { staggerChild, staggerParent } from '@/lib/motion'
import { GOODS } from '@/mocks/data'
import type { Selection } from '@/store/types'

type Props = {
  title: string
  heading: string
  ctaLabel: string
  /** Needs 는 아무것도 안 골라도 넘어갈 수 있다. */
  allowEmpty: boolean
  /** 고를 수 없는 아이템과 그 이유. 내놓기로 한 굿즈를 다시 찾을 수는 없다. */
  disabledItemIds?: string[]
  disabledNote?: string
  selections: Selection[]
  onBack: () => void
  onToggle: (itemId: string) => void
  onChangeQty: (itemId: string, qty: number) => void
  onClear: (itemId: string) => void
  onSubmit: () => void
}

/**
 * Have 와 Needs 는 규칙 두 개(빈 선택 허용 여부)만 다르고 화면이 같다.
 * 같은 화면을 두 번 만들지 않는다.
 */
export function SelectScreen({
  title,
  heading,
  ctaLabel,
  allowEmpty,
  disabledItemIds = [],
  disabledNote,
  selections,
  onBack,
  onToggle,
  onChangeQty,
  onClear,
  onSubmit,
}: Props) {
  const total = selections.reduce((sum, s) => sum + s.qty, 0)
  const disabled = !allowEmpty && total === 0

  return (
    <div className="flex h-full flex-col">
      <TopBar title={title} onBack={onBack} />

      <div className="flex-1 overflow-y-auto px-5 no-scrollbar">
        <div className="flex items-start justify-between pt-3">
          <div>
            <h2 className="text-[17px] font-bold text-ink">{heading}</h2>
            <p className="mt-1 text-[12px] text-neutral-400">꾹 눌러서 선택취소</p>
          </div>
          <motion.p
            key={total}
            initial={{ scale: 0.85, opacity: 0.5 }}
            animate={{ scale: 1, opacity: 1 }}
            className="pt-1 text-[13px] font-semibold text-neutral-500"
          >
            {total}개 선택됨
          </motion.p>
        </div>

        {GOODS.map((goods) => (
          <section key={goods.id} className="mt-6">
            <h3 className="text-[15px] font-bold text-ink">{goods.name}</h3>
            <motion.div
              variants={staggerParent}
              initial="hidden"
              animate="show"
              className="mt-3 grid grid-cols-3 gap-3 sm:grid-cols-4"
            >
              {goods.items.map((item) => {
                const picked = selections.find((s) => s.itemId === item.id)
                const blocked = disabledItemIds.includes(item.id)
                return (
                  <motion.div key={item.id} variants={staggerChild}>
                    <GoodsCard
                      item={item}
                      selected={Boolean(picked)}
                      disabled={blocked}
                      note={blocked ? disabledNote : undefined}
                      qty={picked?.qty}
                      onClick={() => onToggle(item.id)}
                      onIncrease={() => onChangeQty(item.id, (picked?.qty ?? 0) + 1)}
                      onDecrease={() => onChangeQty(item.id, (picked?.qty ?? 1) - 1)}
                      onLongPress={() => onClear(item.id)}
                    />
                  </motion.div>
                )
              })}
            </motion.div>
          </section>
        ))}

        <div className="h-6" />
      </div>

      <div className="shrink-0 px-5 pt-3 pb-8">
        <Button onClick={onSubmit} disabled={disabled}>
          {ctaLabel}
        </Button>
      </div>
    </div>
  )
}
