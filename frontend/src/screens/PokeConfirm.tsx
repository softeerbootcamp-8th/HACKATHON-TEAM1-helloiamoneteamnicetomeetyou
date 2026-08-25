import { motion } from 'motion/react'
import { useNavigate, useSearchParams } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { StatusBar } from '@/components/ui/StatusBar'
import { springSnap } from '@/lib/motion'
import { ALL_WAITING, itemById } from '@/mocks/data'
import { useStore } from '@/store/useStore'

/** 찔러보기를 보내기 전 확인 화면. */
export function PokeConfirm() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { state, dispatch } = useStore()

  const targetId = params.get('to') ?? ''
  const target = ALL_WAITING.find((u) => u.id === targetId)
  const topItemId = state.have[0]?.itemId ?? 'sf'
  const haveCount = state.have.reduce((sum, s) => sum + s.qty, 0)

  if (!target) {
    return (
      <div className="flex h-full flex-col">
        <StatusBar />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8">
          <p className="text-[15px] text-neutral-500">상대를 찾을 수 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  const send = () => {
    dispatch({ type: 'send-poke', targetUserId: target.id })
    navigate('/home')
  }

  return (
    <div className="flex h-full flex-col">
      <StatusBar />

      <div className="flex-1 overflow-y-auto px-6 pt-6 no-scrollbar">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">찔러보기 확인</h1>
        <p className="mt-2 text-[13px] text-neutral-400">상대에게 교환 요청을 보냅니다.</p>

        <div className="mt-10 flex items-center justify-center gap-5">
          <div className="text-center">
            <p className="mb-3 text-[12px] font-semibold text-neutral-400">내 카드 묶음</p>
            <CardStack topItemId={topItemId} count={haveCount} />
            {haveCount > 1 && (
              <p className="mt-3 text-[12px] text-neutral-400">외 {haveCount - 1}장</p>
            )}
          </div>

          <motion.span
            animate={{ x: [0, 6, 0] }}
            transition={{ duration: 1.6, repeat: Infinity, ease: 'easeInOut' }}
            className="text-[22px] text-neutral-300"
          >
            →
          </motion.span>

          <div className="text-center">
            <p className="mb-3 text-[12px] font-semibold text-neutral-400">상대 카드</p>
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={springSnap}
              className="w-[112px] rounded-2xl bg-white p-2.5 shadow-[0_6px_20px_rgba(0,0,0,0.10)]"
            >
              <GoodsFace item={itemById(target.itemId)} size="md" />
              <p className="mt-2 text-center text-[12px] font-bold text-ink">
                {itemById(target.itemId).name}
              </p>
              <p className="text-center text-[11px] text-neutral-400">
                {itemById(target.itemId).nameKo}
              </p>
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
        <Button onClick={send}>찔러보기 보내기</Button>
        <TextButton onClick={() => navigate('/home')}>취소</TextButton>
      </div>
    </div>
  )
}
