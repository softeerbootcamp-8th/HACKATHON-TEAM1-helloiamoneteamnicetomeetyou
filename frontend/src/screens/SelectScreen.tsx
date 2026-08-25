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
  /** 서버에 보내는 중. 두 번 눌려 두 번 등록되는 것을 막는다. */
  submitting?: boolean
  /** 등록에 실패한 이유. 버튼 위에 그대로 띄운다. */
  submitError?: string
  /** 서버 준비 상태 안내. 아직 등록할 수 없을 때 이유를 알려 준다. */
  notice?: string
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
  submitting = false,
  submitError,
  notice,
  onBack,
  onToggle,
  onChangeQty,
  onClear,
  onSubmit,
}: Props) {
  const total = selections.reduce((sum, s) => sum + s.qty, 0)
  const disabled = submitting || (!allowEmpty && total === 0)

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar title={title} onBack={onBack} />

      <div className="flex-1 overflow-y-auto px-5 no-scrollbar">
        <div className="flex items-start justify-between pt-3">
          <div>
            <h2 className="text-[17px] font-bold text-ink">{heading}</h2>
            {/* 시안 문구다(225:23783 "여러장 선택할 수 있어요"). 꾹 눌러 취소는 화면에
                단서가 없으면 알 수 없어서 같이 남긴다. */}
            <p className="mt-1 text-[12px] text-neutral-400">
              여러장 선택할 수 있어요 · 꾹 눌러서 선택취소
            </p>
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
              // 시안이 한 줄에 3개로 못박아 뒀다(desc 165:3418). 넓은 화면에서 칸을 늘리면
              // 카드가 작아져서 그림과 이름이 시안보다 답답해진다. 대신 폭을 가운데로 모은다.
              className="mx-auto mt-3 grid max-w-[520px] grid-cols-3 gap-3 md:gap-4"
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
        {notice && (
          <p className="mb-2 text-center text-[12px] text-neutral-400" role="status">
            {notice}
          </p>
        )}
        {submitError && (
          <p className="mb-2 text-center text-[12px] text-rose-500" role="alert">
            {submitError}
          </p>
        )}
        <Button onClick={onSubmit} disabled={disabled}>
          {submitting ? '등록하는 중…' : ctaLabel}
        </Button>
      </div>
    </div>
  )
}
