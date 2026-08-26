import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { BreakupDialog } from '@/components/domain/ConfirmDialogs'
import { EmptyState } from '@/components/domain/EmptyState'
import { MoveStatusBadge, OneToOneView, ThreeWayView } from '@/components/domain/ExchangeCards'
import { Button, TextButton } from '@/components/ui/Button'
import { ClockIcon, PinIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { arriveAtExchange } from '@/lib/exchange'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { identityMarkAt, objectParticle, usePrefetchMark } from '@/store/identity-mark'
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
  const arrived = appt.myArrived
  const partnersArrived = appt.partners.every((p) => p.arrived)
  const myUserId = getDeviceId()

  /**
   * "도착했어요". 서버에 남기면 상대 화면의 배지가 이동중에서 도착으로 바뀐다.
   *
   * 실패해도 식별 화면으로는 보낸다. 상대를 찾는 것을 서버 응답 때문에 막을 이유가 없다.
   */
  const goIdentify = async () => {
    setBusy(true)
    try {
      const exchange = await arriveAtExchange(appt.exchangeId, myUserId)
      dispatch({ type: 'exchange-synced', exchange, myUserId })
    } catch {
      dispatch({ type: 'toast', message: '도착을 알리지 못했어요' })
    } finally {
      setBusy(false)
      navigate('/identify')
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
          {remainingLabel(appt.confirmedLabel)}
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
        <Button disabled={busy} onClick={() => void goIdentify()}>
          ‘{identityMarkAt(appt.identityMark).name}’
          {objectParticle(identityMarkAt(appt.identityMark).name)} 찾아볼까요
        </Button>
        <TextButton onClick={() => setCancelOpen(true)}>약속 취소하기</TextButton>
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
 * 약속까지 남은 시간. 시안이 30분을 기준으로 문구를 가른다.
 * 30분보다 가까우면 몇 분 남았는지 적고, 그보다 멀면 "잠시 뒤에 만나요" 로 뭉뚱그린다.
 */
function remainingLabel(confirmedLabel: string | null): string {
  const minutes = minutesUntil(confirmedLabel)
  if (minutes === null || minutes >= 30) return '잠시 뒤에 만나요'
  return `${Math.max(minutes, 1)}분 뒤에 만나요`
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
