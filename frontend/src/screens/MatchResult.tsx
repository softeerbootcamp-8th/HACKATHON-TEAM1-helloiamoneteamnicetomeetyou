import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { springSnap } from '@/lib/motion'
import { itemById, MY_IDENTITY } from '@/mocks/data'
import { useStore } from '@/store/useStore'

/**
 * 매칭 결과. 1:1 과 삼자 교환이 한 화면에서 갈린다.
 * 자동 매칭으로 잡힌 것과 찔러보기로 성사된 것은 제목이 다르다.
 */
export function MatchResult() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { state, dispatch } = useStore()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const match = state.match
  const demo = params.get('demo')

  // 주소로 바로 열었을 때 화면을 볼 수 있게 상태를 심어 준다.
  useEffect(() => {
    if (demo === '3way' && !match) dispatch({ type: 'seed-demo', kind: 'three-way' })
  }, [demo, match, dispatch])

  if (!match) {
    return (
      <div className="flex h-full flex-col">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="text-[15px] text-neutral-500">진행 중인 매칭이 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  const headline =
    match.origin === 'poke'
      ? '이렇게 교환할게요'
      : match.kind === 'ONE_TO_ONE'
        ? '서로 원하는 카드가\n정확히 맞았어요'
        : '셋이 교환하면\n모두 원하는 걸 얻어요'

  const sub =
    match.kind === 'ONE_TO_ONE'
      ? '상대와 교환할 카드를 확인하세요.'
      : '아래와 같이 카드가 교환돼요.'

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto px-6 pt-8 no-scrollbar">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={springSnap}
          className="text-[26px] leading-[1.34] font-extrabold whitespace-pre-line tracking-[-0.02em] text-ink"
        >
          {headline}
        </motion.h1>
        <p className="mt-3 text-[13px] text-neutral-400">{sub}</p>

        {match.kind === 'ONE_TO_ONE' ? (
          <OneToOneView giveItemId={match.giveItemId} receiveItemId={match.receiveItemId} />
        ) : (
          <ThreeWayView
            myItemId={match.giveItemId}
            giverNickname={match.giver.nickname}
            giverItemId={match.receiveItemId}
            receiverNickname={match.receiver.nickname}
            receiverItemId={match.middleItemId}
          />
        )}
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button
          onClick={() => {
            dispatch({ type: 'start-appointment' })
            navigate('/place')
          }}
        >
          교환 장소보기
        </Button>
        <TextButton onClick={() => setConfirmOpen(true)}>거절하기</TextButton>
      </div>

      <Dialog
        open={confirmOpen}
        title="교환을 거절할까요?"
        description="거절하면 다시 상대를 찾습니다."
        cancelLabel="아니요"
        confirmLabel="거절할게요"
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => {
          setConfirmOpen(false)
          dispatch({ type: 'decline-match' })
          navigate('/home')
        }}
      />
    </div>
  )
}

function ExchangeCard({ itemId, label }: { itemId: string; label: string }) {
  const item = itemById(itemId)
  return (
    <div className="text-center">
      <p className="mb-3 text-[12px] font-bold text-ink">{label}</p>
      <motion.div
        initial={{ opacity: 0, y: 16, scale: 0.94 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={springSnap}
        className="w-[124px] rounded-2xl bg-white p-3 shadow-[0_6px_22px_rgba(0,0,0,0.10)]"
      >
        <GoodsFace item={item} size="lg" />
        <p className="mt-2.5 text-center text-[12px] font-bold text-ink">{item.name}</p>
        <p className="text-center text-[11px] text-neutral-400">{item.nameKo}</p>
      </motion.div>
    </div>
  )
}

function OneToOneView({
  giveItemId,
  receiveItemId,
}: {
  giveItemId: string
  receiveItemId: string
}) {
  return (
    <div className="mt-10 flex items-center justify-center gap-3">
      <ExchangeCard itemId={giveItemId} label="내가 주는 카드" />
      <span className="anim-breathe mt-6 text-[20px] text-ink">⇄</span>
      <ExchangeCard itemId={receiveItemId} label="내가 받는 카드" />
    </div>
  )
}

function ThreeWayView({
  myItemId,
  giverNickname,
  giverItemId,
  receiverNickname,
  receiverItemId,
}: {
  myItemId: string
  giverNickname: string
  giverItemId: string
  receiverNickname: string
  receiverItemId: string
}) {
  return (
    <div className="mt-8">
      <div className="flex justify-center">
        <ExchangeCard itemId={myItemId} label={`나 (${MY_IDENTITY.fruit} ${MY_IDENTITY.number})`} />
      </div>

      <div className="mt-3 flex items-center justify-center gap-24 text-[18px] text-brand">
        <span className="anim-float-sm">↗</span>
        <span className="anim-float-sm" style={{ animationDelay: '0.8s' }}>
          ↘
        </span>
      </div>

      <div className="mt-3 flex items-start justify-center gap-3">
        <ExchangeCard itemId={giverItemId} label={giverNickname} />
        <span className="anim-nudge-x-back mt-16 text-[18px] text-brand">←</span>
        <ExchangeCard itemId={receiverItemId} label={receiverNickname} />
      </div>
    </div>
  )
}
