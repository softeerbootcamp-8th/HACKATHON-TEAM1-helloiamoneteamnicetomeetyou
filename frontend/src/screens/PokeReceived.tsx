import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { GoodsCard, GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { staggerChild, staggerParent } from '@/lib/motion'
import { itemById } from '@/mocks/data'
import { useLastDefined } from '@/lib/useLastDefined'
import { useStore } from '@/store/useStore'

/** 받은 교환 요청. 상대의 묶음에서 한 장을 고른다. */
export function PokeReceived() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { state, dispatch } = useStore()
  const incoming = useLastDefined(state.incomingPoke)
  const [picked, setPicked] = useState<string | null>(null)
  const demo = params.get('demo')

  // 주소로 바로 열었을 때 화면을 볼 수 있게 상태를 심어 준다.
  useEffect(() => {
    if (demo && !incoming) dispatch({ type: 'seed-demo', kind: 'incoming' })
  }, [demo, incoming, dispatch])

  // 아직 아무것도 안 골랐으면 첫 장을 고른 것으로 본다. effect 로 setState 하지 않고
  // 렌더할 때 계산하면 상태가 하나 줄어든다.
  const chosen = picked ?? incoming?.offeredItemIds[0] ?? null

  if (!incoming) {
    return (
      <div className="flex h-full flex-col">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="text-[15px] text-neutral-500">받은 교환 요청이 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto px-6 pt-6 no-scrollbar">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">
          상대가 교환을 요청했어요
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">
          상대의 카드 묶음에서 원하는 1장을 선택할 수 있어요
        </p>

        <section className="mt-7">
          <h2 className="text-[13px] font-bold text-ink">상대가 원하는 카드</h2>
          <div className="mt-3 w-[118px] rounded-2xl bg-white p-2.5 shadow-[0_4px_16px_rgba(0,0,0,0.08)]">
            <GoodsFace item={itemById(incoming.wantItemId)} size="md" />
            <p className="mt-2 text-center text-[12px] font-bold text-ink">
              {itemById(incoming.wantItemId).name}
            </p>
            <p className="text-center text-[11px] text-neutral-400">
              {itemById(incoming.wantItemId).nameKo}
            </p>
          </div>
        </section>

        <section className="mt-8">
          <h2 className="text-[13px] font-bold text-ink">상대의 카드 묶음 (1장 선택)</h2>
          <motion.div
            variants={staggerParent}
            initial="hidden"
            animate="show"
            className="mt-3 grid grid-cols-3 gap-3"
          >
            {incoming.offeredItemIds.map((id) => (
              <motion.div key={id} variants={staggerChild}>
                <GoodsCard
                  item={itemById(id)}
                  selected={chosen === id}
                  onClick={() => setPicked(id)}
                />
              </motion.div>
            ))}
          </motion.div>
        </section>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button
          disabled={!chosen}
          onClick={() => {
            if (!chosen) return
            dispatch({ type: 'accept-incoming', chosenItemId: chosen })
            navigate('/match')
          }}
        >
          이 카드로 진행하기
        </Button>
        <TextButton
          onClick={() => {
            dispatch({ type: 'reject-incoming' })
            navigate('/home')
          }}
        >
          거절하기
        </TextButton>
      </div>
    </div>
  )
}
