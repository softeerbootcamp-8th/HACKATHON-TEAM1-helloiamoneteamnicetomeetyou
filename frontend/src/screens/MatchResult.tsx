import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { RejectDialog } from '@/components/domain/ConfirmDialogs'
import { EmptyState } from '@/components/domain/EmptyState'
import { OneToOneView, ThreeWayView } from '@/components/domain/ExchangeCards'
import { Button, TextButton } from '@/components/ui/Button'
import { TopBar } from '@/components/ui/TopBar'
import { acceptExchange, rejectExchange } from '@/features/matching/api'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { fetchExchange } from '@/lib/exchange'
import { getDeviceId } from '@/store/identity'
import { useStore } from '@/store/useStore'

/**
 * 매칭 결과. 1:1 과 삼자 교환이 한 화면에서 갈린다.
 *
 * 자동 매칭으로 잡힌 것과 찔러보기로 성사된 것은 제목뿐 아니라 빠져나가는 길이 다르다.
 * 자동 매칭은 아직 거절할 수 있고, 찔러보기 성사는 이미 서로 합의한 자리라 거절 대신
 * 뒤로가기만 둔다.
 */
export function MatchResult() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { state, dispatch } = useStore()
  const [rejectOpen, setRejectOpen] = useState(false)
  const match = useLastDefined(state.match)
  const [accepting, setAccepting] = useState(false)

  /**
   * 매칭 결과를 받아들이고 장소 화면으로 넘어간다.
   *
   * 서버가 이때 만날 자리와 시간 격자, 약속 식별자를 붙인다. 매칭이 교환을 만드는 시점에는
   * 아직 제안일 뿐이라 그것들이 비어 있다.
   *
   * 상대의 수락을 기다리지 않는다. 각자 장소와 시간 화면으로 들어가 맞춰 보는 흐름이다.
   */
  const goToPlace = async () => {
    if (!match) return

    // 목업으로 심어 준 매칭은 서버에 교환이 없다. 화면만 넘긴다.
    if (match.exchangeId === null) {
      navigate('/place')
      return
    }

    const myUserId = getDeviceId()
    setAccepting(true)
    try {
      await acceptExchange(match.exchangeId, myUserId)

      // 수락한 직후의 약속을 바로 읽어 둔다. 알림을 기다리면 장소 화면이 잠깐 비어 보인다.
      const exchange = await fetchExchange(match.exchangeId)
      dispatch({ type: 'exchange-synced', exchange, myUserId, match, activate: true })
      navigate('/place')
    } catch {
      dispatch({ type: 'toast', message: '교환 장소를 열지 못했어요. 잠시 후 다시 시도해주세요' })
    } finally {
      setAccepting(false)
    }
  }

  const demo = params.get('demo')

  // 주소로 바로 열었을 때 화면을 볼 수 있게 상태를 심어 준다.
  useEffect(() => {
    if (demo === '3way' && !match) dispatch({ type: 'seed-demo', kind: 'three-way' })
  }, [demo, match, dispatch])

  if (!match) {
    return (
      <EmptyState
        title="진행 중인 매칭이 없어요"
        description={'교환 대기장에서 원하는 카드를\n먼저 찔러보세요.'}
        onAction={() => navigate('/home')}
      />
    )
  }

  const fromPoke = match.origin === 'poke'

  const headline = fromPoke
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
      {/* 찔러보기로 성사된 화면에만 뒤로가기가 있다. 자동 매칭은 거절이 그 자리를 대신한다. */}
      {fromPoke && <TopBar onBack={() => navigate('/home')} />}

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
            <OneToOneView pairs={match.pairs} />
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
        <Button disabled={accepting} onClick={() => void goToPlace()}>
          {accepting ? '교환 장소를 여는 중' : '교환 장소 확인하기'}
        </Button>
        {!fromPoke && <TextButton onClick={() => setRejectOpen(true)}>거절하기</TextButton>}
      </div>

      <RejectDialog
        open={rejectOpen}
        onKeep={() => setRejectOpen(false)}
        onReject={() => {
          setRejectOpen(false)
          if (match.exchangeId !== null) {
            rejectExchange(match.exchangeId, getDeviceId()).catch((error: unknown) =>
              console.error('[exchange] 거절 실패', error),
            )
          }
          dispatch({ type: 'decline-match' })
          navigate('/home')
        }}
      />
    </div>
  )
}
