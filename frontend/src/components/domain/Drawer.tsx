import { AnimatePresence, motion } from 'motion/react'

import { easeOut, springSheet } from '@/lib/motion'
import { itemById } from '@/mocks/data'
import type { Selection } from '@/store/types'

type Props = {
  open: boolean
  onClose: () => void
  have: Selection[]
  needs: Selection[]
  onEditHave: () => void
  onEditNeeds: () => void
  onReset: () => void
}

/** 햄버거 메뉴. 내 카드를 확인하고 Have/Needs 를 고치러 가는 자리다. */
export function Drawer({ open, onClose, have, needs, onEditHave, onEditNeeds, onReset }: Props) {
  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.button
            type="button"
            aria-label="메뉴 닫기"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={easeOut}
            className="absolute inset-0 z-40 bg-black/30"
          />
          <motion.aside
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={springSheet}
            className="absolute inset-y-0 right-0 z-50 flex w-[78%] max-w-[320px] flex-col bg-white shadow-2xl"
          >
            <div className="flex h-14 items-center justify-between px-5">
              <h2 className="text-[17px] font-bold text-ink">내 카드</h2>
              <button
                type="button"
                aria-label="닫기"
                onClick={onClose}
                className="text-[24px] font-light text-ink"
              >
                ✕
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-5 no-scrollbar">
              <Section title="내놓을 굿즈" selections={have} onEdit={onEditHave} />
              <Section title="찾는 굿즈" selections={needs} onEdit={onEditNeeds} />
            </div>

            <div className="px-5 pb-8">
              <button
                type="button"
                onClick={onReset}
                className="w-full rounded-2xl border border-neutral-200 py-3 text-[14px] font-semibold text-neutral-500"
              >
                처음부터 다시 하기
              </button>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  )
}

function Section({
  title,
  selections,
  onEdit,
}: {
  title: string
  selections: Selection[]
  onEdit: () => void
}) {
  return (
    <section className="mt-4">
      <div className="flex items-center justify-between">
        <h3 className="text-[14px] font-bold text-ink">{title}</h3>
        <button
          type="button"
          onClick={onEdit}
          className="text-[13px] font-semibold text-neutral-400"
        >
          수정
        </button>
      </div>
      {selections.length === 0 ? (
        <p className="mt-2 text-[13px] text-neutral-400">아직 고른 굿즈가 없어요</p>
      ) : (
        <ul className="mt-2 space-y-1.5">
          {selections.map((s) => (
            <li
              key={s.itemId}
              className="flex items-center justify-between rounded-xl bg-neutral-50 px-3 py-2.5"
            >
              <span className="text-[13px] font-semibold text-ink">{itemById(s.itemId).name}</span>
              <span className="text-[13px] text-neutral-400">{s.qty}장</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
