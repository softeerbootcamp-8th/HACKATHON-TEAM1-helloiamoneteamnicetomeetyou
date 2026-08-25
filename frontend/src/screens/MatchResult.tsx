import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { cn } from '@/lib/cn'
import { createExchange } from '@/lib/exchange'
import { springSnap } from '@/lib/motion'
import { itemById, MY_IDENTITY } from '@/mocks/data'
import { useLastDefined } from '@/lib/useLastDefined'
import { getDeviceId } from '@/store/identity'
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
  const [creating, setCreating] = useState(false)
  const match = useLastDefined(state.match)
  const demo = params.get('demo')

  /**
   * 교환을 서버에 만들고 약속 화면으로 넘어간다.
   *
   * 상대는 아직 화면이 목업으로 고르지만, 그 사람들은 서버에 실제로 있는 사용자다. 여기서
   * 만들어진 교환에 시간과 장소가 붙고, 참가자들은 실시간으로 서로의 선택을 본다.
   *
   * 매칭이 서버로 옮겨가면 이 호출은 사라진다. 교환은 매칭 결과 알림으로 내려오게 된다.
   */
  const goToPlace = async () => {
    // 뒤로 갔다 다시 들어온 경우다. 또 만들면 같은 상대와의 교환이 하나 더 생긴다.
    if (state.appointment) {
      navigate('/place')
      return
    }

    if (!match || state.boothId === null) {
      dispatch({ type: 'toast', message: '서버에 연결하지 못했어요. 잠시 후 다시 시도해주세요' })
      return
    }

    const partnerUserIds =
      match.kind === 'ONE_TO_ONE'
        ? [match.partner.userId]
        : [match.giver.userId, match.receiver.userId]

    setCreating(true)
    try {
      const myUserId = getDeviceId()
      const exchange = await createExchange({
        boothId: state.boothId,
        type: match.kind === 'ONE_TO_ONE' ? 'ONE_TO_ONE' : 'MULTI_WAY',
        participantUserIds: [myUserId, ...partnerUserIds],
      })
      dispatch({ type: 'exchange-synced', exchange, myUserId })
      navigate('/place')
    } catch {
      dispatch({ type: 'toast', message: '교환을 시작하지 못했어요. 잠시 후 다시 시도해주세요' })
    } finally {
      setCreating(false)
    }
  }

  // 주소로 바로 열었을 때 화면을 볼 수 있게 상태를 심어 준다.
  useEffect(() => {
    if (demo === '3way' && !match) dispatch({ type: 'seed-demo', kind: 'three-way' })
  }, [demo, match, dispatch])

  if (!match) {
    return (
      <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
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
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      {/*
        데스크톱에서는 스크롤 없이 한 화면에 다 들어와야 한다. 삼자 교환은 카드가
        세 장이라 세로로 길어서, 넓은 화면에서는 가운데로 모으고 넘침을 막는다.
      */}
      <div className="flex-1 overflow-y-auto px-6 pt-8 no-scrollbar md:flex md:flex-col md:overflow-hidden">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={springSnap}
          className="shrink-0 text-[26px] leading-[1.34] font-extrabold whitespace-pre-line tracking-[-0.02em] text-ink"
        >
          {headline}
        </motion.h1>
        <p className="mt-3 shrink-0 text-[13px] text-neutral-400">{sub}</p>

        {/* 제목은 위에 붙이고 카드만 남는 공간 가운데에 놓는다. 통째로 가운데 정렬하면
            제목이 다른 화면보다 아래로 내려가서 화면마다 위치가 달라 보인다. */}
        <div className="md:flex md:min-h-0 md:flex-1 md:items-center md:justify-center">
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
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button disabled={creating} onClick={() => void goToPlace()}>
          {creating ? '교환을 시작하는 중' : '교환 장소보기'}
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

function ExchangeCard({
  itemId,
  label,
  compact = false,
}: {
  itemId: string
  label: string
  /** 카드 세 장이 세로로 쌓이는 자리. 데스크톱에서 화면을 넘기지 않게 줄인다. */
  compact?: boolean
}) {
  const item = itemById(itemId)
  return (
    <div className="text-center">
      <p className="mb-3 text-[12px] font-bold text-ink">{label}</p>
      <motion.div
        initial={{ opacity: 0, y: 16, scale: 0.94 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={springSnap}
        className={cn(
          'rounded-2xl bg-white p-3 shadow-[0_6px_22px_rgba(0,0,0,0.10)]',
          compact ? 'w-[124px] md:w-[112px]' : 'w-[124px] md:w-[140px]',
        )}
      >
        <GoodsFace item={item} size={compact ? 'md' : 'lg'} />
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
    <div className="mt-10 flex items-center justify-center gap-3 md:mt-0">
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
    <div className="mt-8 md:mt-0">
      <div className="flex justify-center">
        <ExchangeCard
          compact
          itemId={myItemId}
          label={`나 (${MY_IDENTITY.fruit} ${MY_IDENTITY.number})`}
        />
      </div>

      <div className="mt-3 flex items-center justify-center gap-24 text-[18px] text-brand md:mt-1">
        <span className="anim-float-sm">↗</span>
        <span className="anim-float-sm" style={{ animationDelay: '0.8s' }}>
          ↘
        </span>
      </div>

      <div className="mt-3 flex items-start justify-center gap-3 md:mt-1">
        <ExchangeCard compact itemId={giverItemId} label={giverNickname} />
        <span className="anim-nudge-x-back mt-16 text-[18px] text-brand">←</span>
        <ExchangeCard compact itemId={receiverItemId} label={receiverNickname} />
      </div>
    </div>
  )
}
