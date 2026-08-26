import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { EmptyState } from '@/components/domain/EmptyState'
import { ItemCard } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { useCatalog } from '@/features/catalog/useCatalog'
import type { BoothHaveItem } from '@/features/poke/api'
import { usePoke } from '@/features/poke/usePoke'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { useTopHaveItemId } from '@/store/top-card'
import { useStore } from '@/store/useStore'

/**
 * 찔러보기를 보내기 전 확인 화면.
 *
 * 주소의 `to` 는 부스 목록의 보유 등록 줄 id 다. 그 줄을 못 찾으면 목록이 갱신되면서
 * 사라진 것이라 고르는 자리로 돌려보낸다.
 */
export function PokeConfirm() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { waiting } = usePoke()
  const to = params.get('to') ?? ''

  // 나가는 중에는 목록이 이미 바뀌어 있어서, 처음 잡은 상대를 계속 들고 있는다.
  const target = useLastDefined(waiting.find((row) => String(row.haveItemId) === to))

  if (!target) {
    return (
      <EmptyState
        title="상대를 찾을 수 없어요"
        description={'교환 대기장에서\n다시 골라 주세요.'}
        onAction={() => navigate('/home')}
      />
    )
  }

  return <ConfirmView target={target} />
}

/** 상대에게 실제로 보낸다. */
function ConfirmView({ target }: { target: BoothHaveItem }) {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { send } = usePoke()
  const { state: catalog } = useCatalog()
  const [submitting, setSubmitting] = useState(false)

  const targetItem = catalog.status === 'ready' ? catalog.itemById(target.item.id) : undefined
  const topItemId = useTopHaveItemId(state.have)
  const haveCount = state.have.reduce((sum, s) => sum + s.qty, 0)

  /**
   * 보내고 교환 대기장으로 돌아간다.
   *
   * 돌아간 자리에서 토스트로 무엇을 보냈는지 알린다 (시안 `토스트 정리` 204:5148 의
   * "찔러보기 제안한 경우"). 화면이 바뀌면서 요청이 나갔다는 흔적이 사라지기 때문에,
   * 이 한 줄이 없으면 아무 일도 없던 것처럼 보인다.
   */
  const submit = async () => {
    if (submitting) return
    setSubmitting(true)
    try {
      await send(target.ownerId, target.item.id)
      dispatch({
        type: 'toast',
        message: `${target.item.name} 교환을 제안했어요\n답변 기다리는 중`,
      })
      navigate('/home')
    } catch {
      // 사유는 PokeProvider 가 들고 있고 홈 화면이 띄운다. 되돌아가지 않는다.
      setSubmitting(false)
    }
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
              {targetItem ? (
                <ItemCard
                  item={targetItem}
                  size="md"
                  className="shadow-[0_6px_20px_rgba(0,0,0,0.10)]"
                />
              ) : (
                // 목록을 아직 못 받았거나 이 부스에 없는 카드다. 그림은 못 그려도 무엇을
                // 요청하는지는 보여야 한다.
                <p className="rounded-2xl bg-white py-8 text-center text-[12px] font-bold text-ink ring-1 ring-line">
                  {target.item.name}
                </p>
              )}
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
        <Button disabled={submitting} onClick={submit}>
          {submitting ? '보내는 중...' : '교환 요청 보내기'}
        </Button>
        <TextButton onClick={() => navigate('/home')}>취소</TextButton>
      </div>
    </div>
  )
}
