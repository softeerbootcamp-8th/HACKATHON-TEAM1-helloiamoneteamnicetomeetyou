import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'

import { BreakupDialog } from '@/components/domain/ConfirmDialogs'
import { EmptyState } from '@/components/domain/EmptyState'
import { MoveStatusBadge, OneToOneView, ThreeWayView } from '@/components/domain/ExchangeCards'
import { Button, TextButton } from '@/components/ui/Button'
import { ClockIcon, PinIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { arriveAtExchange, fetchExchange } from '@/lib/exchange'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { identityMarkAt, usePrefetchMark } from '@/store/identity-mark'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { getDeviceId } from '@/store/identity'
import { activeAppointment } from '@/store/reducer'
import { useStore } from '@/store/useStore'

/**
 * 확정된 약속. 남은 시간과 장소, 주고받을 카드를 다시 보여준다.
 * 닫기는 약속을 그대로 둔 채 교환 대기장소로 돌아간다. 약속까지 시간이 남았을 때
 * 다른 카드를 더 찔러볼 수 있어야 하기 때문이다.
 */
export function Appointment() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const [cancelOpen, setCancelOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const cancelAppointment = useCancelAppointment()
  const appt = useLastDefined(activeAppointment(state))
  // hook 은 early return 위에 있어야 한다. 아래 빈 화면으로 빠질 때 호출 수가 달라지면 터진다.
  usePrefetchMark(appt?.identityMark ?? null)

  const arrived = appt?.myArrived ?? false
  const partnersArrived = appt?.partners.every((p) => p.arrived) ?? false
  const allArrived = arrived && partnersArrived
  const waiting = appt?.stage === 'confirmed' || appt?.stage === 'arrived'

  /*
    전원이 도착하면 서로를 찾는 화면으로 넘어간다. "만났어요!" 가 거기 있다.

    마지막으로 누른 사람은 자기가 누른 결과로, 먼저 도착해 기다리던 사람은 EXCHANGE_ARRIVED 가
    들어오는 순간 같이 넘어간다. 누른 사람만 넘어가면 먼저 와서 기다리던 쪽은 도착 배지만 보면서
    계속 서 있게 된다.

    이미 끝난 약속은 넘기지 않는다. 교환을 마친 뒤 이 주소로 돌아오면 식별 화면으로 튕겼다가
    거기서 다시 완료 화면으로 넘어가는 길이 생긴다.
  */
  useEffect(() => {
    if (allArrived && waiting) navigate('/identify')
  }, [allArrived, waiting, navigate])

  if (!appt || (appt.stage !== 'confirmed' && appt.stage !== 'arrived')) {
    return (
      <EmptyState
        title="확정된 약속이 없어요"
        description={'장소와 시간을 정하면\n여기에서 다시 볼 수 있어요.'}
        icon={<ClockIcon className="size-9" />}
        onAction={() => navigate('/home')}
      />
    )
  }

  const { zone, match } = appt
  const myUserId = getDeviceId()

  /**
   * "도착했어요". 서버에 남기면 상대 화면의 배지가 이동중에서 도착으로 바뀐다.
   *
   * **여기서 곧바로 식별 화면으로 넘어가지 않는다.** 식별 화면은 같은 표시를 든 사람끼리 서로를
   * 알아보는 자리라, 아직 오는 중인 사람이 있으면 화면을 들고 있어도 맞은편에 아무도 없다.
   * 넘어가는 것은 전원이 도착한 순간이고, 그 판단은 위의 effect 가 한다.
   *
   * 실패하면 서버에서 현재 상태를 다시 읽는다. 화면만 도착으로 바뀌면 상대에게는 여전히 오는
   * 중으로 보여서, 서로 다른 화면을 든 채 기다리게 된다.
   */
  const markArrived = async () => {
    setBusy(true)
    try {
      const exchange = await arriveAtExchange(appt.exchangeId, myUserId)
      dispatch({ type: 'exchange-synced', exchange, myUserId })
    } catch {
      const latest = await fetchExchange(appt.exchangeId).catch(() => null)
      if (latest) dispatch({ type: 'exchange-synced', exchange: latest, myUserId })
      dispatch({ type: 'toast', message: '도착을 알리지 못했어요' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar onClose={() => navigate('/home')} />

      <div className="flex-1 overflow-y-auto px-6 no-scrollbar">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={springSnap}
          className="text-[26px] font-extrabold tracking-[-0.02em] text-ink"
        >
          {remainingLabel(appt.confirmedLabel, identityMarkAt(appt.identityMark).emoji)}
        </motion.h1>

        <motion.div
          initial={{ opacity: 0, y: 14, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ ...springSnap, delay: 0.06 }}
          className="mt-4 flex items-center gap-3 rounded-2xl border-2 border-ink p-3.5"
        >
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-white">
            <PinIcon className="size-5" />
          </span>
          <div>
            <p className="text-[16px] font-bold text-ink">
              {appt.confirmedLabel} {zone.name}
            </p>
            <p className="text-[12px] text-neutral-400">{zone.location}</p>
          </div>
        </motion.div>

        <div className="mt-10">
          {/* 무엇을 주고받는지는 서버가 약속에 담아 주지 않아서, 없으면 도착 여부만 보여준다. */}
          {!match ? (
            <div className="flex items-center justify-center gap-6">
              <div className="text-center">
                <MoveStatusBadge arrived={arrived} />
                <p className="mt-2.5 text-[12px] text-neutral-500">나</p>
              </div>
              <span className="anim-breathe text-[20px] text-ink">⇄</span>
              <div className="text-center">
                <MoveStatusBadge arrived={partnersArrived} />
                <p className="mt-2.5 text-[12px] text-neutral-500">
                  {appt.partners.map((p) => p.name).join(', ') || '상대'}
                </p>
              </div>
            </div>
          ) : match.kind === 'ONE_TO_ONE' ? (
            <OneToOneView
              pairs={match.pairs}
              giveBadge={<MoveStatusBadge arrived={arrived} />}
              receiveBadge={<MoveStatusBadge arrived={partnersArrived} />}
            />
          ) : (
            <ThreeWayView
              myItemId={match.giveItemId}
              giverNickname={match.giver.nickname}
              giverItemId={match.receiveItemId}
              receiverNickname={match.receiver.nickname}
              receiverItemId={match.middleItemId}
              myBadge={<MoveStatusBadge arrived={arrived} />}
              giverBadge={<MoveStatusBadge arrived={appt.partners[0]?.arrived ?? false} />}
              receiverBadge={<MoveStatusBadge arrived={appt.partners[1]?.arrived ?? false} />}
            />
          )}
        </div>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        {/*
          CTA 는 두 갈래다. 아직 도착을 안 알렸으면 알리고, 알렸으면 남은 사람을 기다린다.
          전원이 도착하는 순간 위의 effect 가 식별 화면으로 넘기기 때문에 세 번째 갈래가 없다.

          상대의 도착은 EXCHANGE_ARRIVED 로 들어와 화면이 알아서 바뀐다. 기다리는 사람이
          새로고침할 일은 없다.
        */}
        {arrived ? (
          <Button disabled>상대의 도착을 기다리는 중이에요</Button>
        ) : (
          <Button disabled={busy} onClick={() => void markArrived()}>
            도착했어요
          </Button>
        )}
        <TextButton onClick={() => setCancelOpen(true)}>약속 취소</TextButton>
      </div>

      <BreakupDialog
        open={cancelOpen}
        onKeep={() => setCancelOpen(false)}
        onFindNew={() => {
          setCancelOpen(false)
          void cancelAppointment().then((cancelled) => {
            // 상대가 먼저 교환을 마쳤으면 취소가 안 된다. 그때는 화면이 그 결과를 따라간다.
            if (cancelled) navigate('/home')
          })
        }}
      />
    </div>
  )
}

/**
 * 약속까지 남은 시간. UX 라이팅 정리판의 `{남은시간}분 뒤에 만나요` 규칙이다.
 *
 * 아직 먼 약속에 분을 세어 주면 "97분 뒤" 같은 읽기 어려운 수가 나와서, 한 시간이 넘으면
 * 그냥 약속 시각을 적는다. 반대로 코앞이면 분을 세는 것보다 고개를 드는 것이 먼저라
 * 과일과 함께 "곧 만나요" 로 바꾼다.
 */
function remainingLabel(confirmedLabel: string | null, emoji: string): string {
  const minutes = minutesUntil(confirmedLabel)
  if (minutes === null) return '잠시 뒤에 만나요'
  if (minutes < 0) return '지금 만나는 중이에요'
  if (minutes <= 2) return `곧 만나요! ${emoji}`
  if (minutes <= 60) return `${minutes}분 뒤에 만나요`
  return `${confirmedLabel}에 만나요`
}

/** "2:45" 같은 라벨을 오늘 그 시각으로 읽고 지금까지 몇 분 남았는지 센다. */
function minutesUntil(confirmedLabel: string | null): number | null {
  if (!confirmedLabel) return null
  const [hour, minute] = confirmedLabel.split(':').map(Number)
  if (Number.isNaN(hour) || Number.isNaN(minute)) return null

  const at = new Date()
  at.setHours(hour, minute, 0, 0)
  return Math.round((at.getTime() - Date.now()) / 60_000)
}
