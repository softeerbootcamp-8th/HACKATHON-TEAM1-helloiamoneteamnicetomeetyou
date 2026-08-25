import { motion } from 'motion/react'
import { useNavigate, useSearchParams } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { EmptyState } from '@/components/domain/EmptyState'
import { ItemCard } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { springSnap } from '@/lib/motion'
import { ALL_WAITING, itemById } from '@/mocks/data'
import { useLastDefined } from '@/lib/useLastDefined'
import { useStore } from '@/store/useStore'

/** 찔러보기를 보내기 전 확인 화면. */
export function PokeConfirm() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { state, dispatch } = useStore()

  // 나가는 중에는 주소가 이미 다음 화면 것이라, 처음 잡은 상대를 계속 들고 있는다.
  const target = useLastDefined(ALL_WAITING.find((u) => u.id === (params.get('to') ?? '')))
  const topItemId = state.have[0]?.itemId ?? 'avn'
  const haveCount = state.have.reduce((sum, s) => sum + s.qty, 0)

  if (!target) {
    return (
      <EmptyState
        title="상대를 찾을 수 없어요"
        description={'교환 대기장에서\n다시 골라 주세요.'}
        onAction={() => navigate('/home')}
      />
    )
  }

  const send = () => {
    dispatch({ type: 'send-poke', targetUserId: target.id })
    navigate('/home')
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex-1 overflow-y-auto px-6 pt-6 no-scrollbar">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">
          교환 요청을 보내시겠어요?
        </h1>

        <div className="mt-12 flex items-start justify-center gap-5">
          <div className="text-center">
            <p className="mb-3 text-[12px] font-semibold text-neutral-400">내 카드 묶음</p>
            <CardStack topItemId={topItemId} count={haveCount} />
            {haveCount > 1 && (
              <p className="mt-3 text-[12px] text-neutral-400">외 {haveCount - 1}장</p>
            )}
          </div>

          <span className="anim-nudge-x mt-[100px] text-[22px] text-brand">→</span>

          <div className="text-center">
            <p className="mb-3 text-[12px] font-semibold text-neutral-400">상대 카드</p>
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={springSnap}
              className="w-[112px]"
            >
              <ItemCard
                item={itemById(target.itemId)}
                size="md"
                className="shadow-[0_6px_20px_rgba(0,0,0,0.10)]"
              />
            </motion.div>
          </div>
        </div>

        <div className="mt-12 flex items-start gap-2.5 rounded-2xl bg-neutral-50 p-4">
          <span className="text-[14px] text-neutral-400">ⓘ</span>
          <p className="text-[13px] leading-[1.55] text-neutral-500">
            상대는 내 카드 묶음 중 한 장을 선택해 교환을 수락할 수 있어요.
          </p>
        </div>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button onClick={send}>교환 요청 보내기</Button>
        <TextButton onClick={() => navigate('/home')}>취소</TextButton>
      </div>
    </div>
  )
}
