import { motion } from 'motion/react'

import { EmptyState } from '@/components/domain/EmptyState'
import { GoodsCard } from '@/components/domain/GoodsCard'
import { Button } from '@/components/ui/Button'
import { TopBar } from '@/components/ui/TopBar'
import { useCatalog } from '@/features/catalog/useCatalog'
import { staggerChild, staggerParent } from '@/lib/motion'
import type { Selection } from '@/store/types'

type Props = {
  title: string
  heading: string
  ctaLabel: string
  /** 선택 개수 뒤에 붙는 말. 내놓기는 "건넬 수 있어요", 찾기는 "담았어요" 다. */
  countSuffix: string
  /** Needs 는 아무것도 안 골라도 넘어갈 수 있다. */
  allowEmpty: boolean
  /** 고를 수 없는 아이템. 내놓기로 한 굿즈를 다시 찾을 수는 없다. */
  disabledItemIds?: number[]
  selections: Selection[]
  /** 서버에 보내는 중. 두 번 눌려 두 번 등록되는 것을 막는다. */
  submitting?: boolean
  /** 등록에 실패한 이유. 버튼 위에 그대로 띄운다. */
  submitError?: string
  /** 서버 준비 상태 안내. 아직 등록할 수 없을 때 이유를 알려 준다. */
  notice?: string
  onBack: () => void
  onToggle: (itemId: number) => void
  onChangeQty: (itemId: number, qty: number) => void
  onClear: (itemId: number) => void
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
  countSuffix,
  allowEmpty,
  disabledItemIds = [],
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
  const { state: catalog, reload } = useCatalog()
  const total = selections.reduce((sum, s) => sum + s.qty, 0)
  const disabled = submitting || (!allowEmpty && total === 0)

  /*
    카드는 서버가 정한다. 목록을 못 받았으면 고를 것이 없으므로 사유만 보여주고 멈춘다.
    전에는 목업 카드 9장을 그렸는데, 서버에 없는 카드를 고르게 두면 등록도 매칭도 되지 않은
    채로 다음 화면까지 넘어가서 무엇이 잘못됐는지 알 수가 없었다.
  */
  if (catalog.status !== 'ready') {
    return (
      <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
        <TopBar title={title} onBack={onBack} />
        <EmptyState
          title={catalog.status === 'loading' ? '카드를 불러오는 중이에요' : '카드를 볼 수 없어요'}
          description={notice ?? ''}
          actionLabel={catalog.status === 'loading' ? undefined : '다시 시도'}
          onAction={catalog.status === 'loading' ? undefined : reload}
        />
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar title={title} onBack={onBack} />

      <div className="flex-1 overflow-y-auto px-5 no-scrollbar">
        <div className="flex items-start justify-between gap-3 pt-3">
          <div>
            <h2 className="text-[17px] font-bold text-ink">{heading}</h2>
            <p className="mt-1 text-[12px] text-neutral-400">여러 장도 OK!</p>
          </div>
          <motion.p
            key={total}
            initial={{ scale: 0.85, opacity: 0.5 }}
            animate={{ scale: 1, opacity: 1 }}
            className="shrink-0 pt-1 text-[13px] font-semibold text-neutral-500"
          >
            {total}장 {countSuffix}
          </motion.p>
        </div>

        <section className="mt-6">
          <motion.div
            variants={staggerParent}
            initial="hidden"
            animate="show"
            className="mt-3 grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 md:gap-4"
          >
            {catalog.items.map((item) => {
              const picked = selections.find((s) => s.itemId === item.id)
              const blocked = disabledItemIds.includes(item.id)
              return (
                <motion.div key={item.id} variants={staggerChild}>
                  <GoodsCard
                    item={item}
                    selected={Boolean(picked)}
                    disabled={blocked}
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
