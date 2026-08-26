import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { RejectDialog } from '@/components/domain/ConfirmDialogs'
import type { PokeAnswerResult, ReceivedPoke } from '@/features/poke/api'
import { usePoke } from '@/features/poke/usePoke'
import { EmptyState } from '@/components/domain/EmptyState'
import { GoodsCard, ItemCard } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { staggerChild, staggerParent } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { useStore } from '@/store/useStore'

/** 받은 교환 요청. 상대의 묶음에서 한 장을 고른다. */
export function PokeReceived() {
  const navigate = useNavigate()
  const { received, accept, reject } = usePoke()

  // 나가는 중에는 목록이 이미 비어 있어서, 처음 잡은 건을 계속 들고 있는다.
  const poke = useLastDefined(received[0])

  if (!poke) {
    return (
      <EmptyState
        title="받은 교환 요청이 없어요"
        description={'요청이 오면 알림으로\n바로 알려 드릴게요.'}
        onAction={() => navigate('/home')}
      />
    )
  }

  return <ReceivedView poke={poke} onAccept={accept} onReject={reject} />
}

function ReceivedView({
  poke,
  onAccept,
  onReject,
}: {
  poke: ReceivedPoke
  onAccept: (pokeId: number, chosenItemId: number) => Promise<PokeAnswerResult>
  onReject: (pokeId: number) => Promise<void>
}) {
  const navigate = useNavigate()
  const { dispatch } = useStore()
  /*
    고른 카드. 아무것도 고르지 않았으면 진행할 수 없다. 전에는 목록의 첫 카드를 기본값으로
    잡아 뒀는데, 화면에 표시가 붙는 카드가 없어서 "선택이 안 되네" 하고 그냥 진행한 사람이
    엉뚱한 카드로 성사됐다. 여기서 오가는 것은 실제 카드라 되돌릴 수 없다.
  */
  const [picked, setPicked] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [rejectOpen, setRejectOpen] = useState(false)

  /**
   * 수락하고 시안 `7. 찔러보기 성사` 로 넘어간다.
   *
   * <b>서버가 돌려준 값으로 성사 화면을 세운다.</b> 어느 교환이 생겼는지도, 내가 상대
   * 묶음에서 무엇을 골랐는지도 서버만 아는 값이라 화면이 다시 계산할 수 없다.
   * 응답은 답한 사람 기준이라 `giveItemId` 가 그대로 "내가 주는 카드" 다.
   */
  const submitAccept = async () => {
    if (picked === null || submitting) return
    setSubmitting(true)
    try {
      const answer = await onAccept(poke.pokeId, picked)

      if (
        answer.exchangeId !== undefined &&
        answer.giveItemId !== undefined &&
        answer.receiveItemId !== undefined
      ) {
        dispatch({
          type: 'server-poke-matched',
          exchangeId: answer.exchangeId,
          giveItemId: answer.giveItemId,
          receiveItemId: answer.receiveItemId,
          partnerUserId: poke.fromUserId,
          partnerName: poke.fromUserName,
        })
      }
      navigate('/match')
    } catch {
      // 사유는 PokeProvider 가 들고 있고 홈 화면이 띄운다. 여기서는 화면을 붙잡아 둔다.
      setSubmitting(false)
    }
  }

  const submitReject = async () => {
    setRejectOpen(false)
    setSubmitting(true)
    try {
      await onReject(poke.pokeId)
      navigate('/home')
    } catch {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex-1 overflow-y-auto px-6 pt-6 no-scrollbar">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">
          누가 나를 찔러봤어요! 👀
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">
          상대 카드 중 원하는 1장을 고를 수 있어요
        </p>

        <section className="mt-7">
          <h2 className="text-[13px] font-bold text-ink">상대가 원하는 카드</h2>
          <ItemCard item={poke.requestedItem} size="md" className="mt-3 w-[118px]" />
        </section>

        <section className="mt-8">
          <h2 className="text-[13px] font-bold text-ink">상대 카드 (1장 골라요)</h2>
          <motion.div
            variants={staggerParent}
            initial="hidden"
            animate="show"
            className="mt-3 grid grid-cols-3 gap-3"
          >
            {poke.offeredItems.map((item) => (
              <motion.div key={item.id} variants={staggerChild}>
                <GoodsCard
                  item={item}
                  selected={picked === item.id}
                  onClick={() => setPicked(item.id)}
                />
              </motion.div>
            ))}
          </motion.div>
        </section>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button disabled={picked === null || submitting} onClick={submitAccept}>
          {submitting ? '보내는 중...' : '이 카드로 교환할래요'}
        </Button>
        <TextButton onClick={() => setRejectOpen(true)}>다음에요</TextButton>
      </div>

      <RejectDialog open={rejectOpen} onKeep={() => setRejectOpen(false)} onReject={submitReject} />
    </div>
  )
}
