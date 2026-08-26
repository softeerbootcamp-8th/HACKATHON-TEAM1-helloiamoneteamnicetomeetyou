import { AnimatePresence, motion } from 'motion/react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { RejectDialog } from '@/components/domain/ConfirmDialogs'
import { EmptyState } from '@/components/domain/EmptyState'
import { Button, TextButton } from '@/components/ui/Button'
import { ClockIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { cn } from '@/lib/cn'
import {
  confirmExchangeTime,
  fetchExchange,
  resetTimeSlots,
  updateTimeSlots,
  type Exchange,
} from '@/lib/exchange'
import { tick } from '@/lib/haptics'
import { springSheet, springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { getDeviceId } from '@/store/identity'
import { activeAppointment } from '@/store/reducer'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { buildSlots, parseSlotBaseTime, slotTimeLabel, SLOT_COUNT } from '@/store/time'
import { useStore } from '@/store/useStore'

/**
 * 칸 색. 시안에서 뽑은 값이라 임의로 바꾸지 않는다.
 * 클래스 대신 값으로 두는 이유는 색이 바뀔 때 툭 튀지 않고 이어지게 하기 위해서다.
 */
const MY_COLOR = '#2cb3ed'
const PARTNER_COLOR = '#aacae6'
const EMPTY_COLOR = '#f5f5f5'
const OVERLAP_COLOR = '#002c5f'
/** 다른 약속이 이미 차지한 칸. 고를 수 없다는 것이 색으로 먼저 보여야 한다. */
const BLOCKED_COLOR = '#dedede'

export function TimeSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const [rejectOpen, setRejectOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const cancelAppointment = useCancelAppointment()
  const myUserId = useMemo(() => getDeviceId(), [])

  const appt = useLastDefined(activeAppointment(state))

  /*
    격자의 시작점은 서버가 정한다. 화면이 각자 자기 시계로 만들면 14:03 에 연 사람의 0번 칸은
    14:15 이고 14:20 에 연 사람의 0번 칸은 14:30 이라, 같은 칸 번호가 사람마다 다른 시각을 뜻하게
    된다. 참가자 전원이 같은 값을 받아 같은 격자를 본다.
  */
  const baseTime = useMemo(() => (appt ? parseSlotBaseTime(appt.slotBaseTime) : new Date()), [appt])
  const slots = useMemo(() => buildSlots(baseTime), [baseTime])

  if (!appt) {
    return (
      <EmptyState
        title="진행 중인 약속이 없어요"
        description={'교환이 성사되면\n만날 시간을 고를 수 있어요.'}
        icon={<ClockIcon className="size-9" />}
        onAction={() => navigate('/home')}
      />
    )
  }

  /**
   * 이미 확정한 다른 약속이 차지한 칸. 같은 시간에 두 군데를 갈 수 없으니 못 고르게 막는다.
   * 시안의 `11_v2` 자리다.
   *
   * 다른 약속은 격자 시작점이 다를 수 있어서 칸 번호를 그대로 비교하면 안 된다. 확정된 시각을
   * 이 약속의 격자에 다시 얹어 몇 번째 칸인지 구한다.
   */
  const blocked = state.appointments
    .filter((other) => other.exchangeId !== appt.exchangeId && other.confirmedTime !== null)
    .map((other) => slotIndexOf(baseTime, other.confirmedTime as string, appt.slotCount))
    .filter((index): index is number => index !== null)

  /*
    CTA 를 여는 기준은 **상대가 시간을 넣었는가** 다. 내가 아직 안 골랐어도 상대가 골랐으면
    "시간 조율 요청하기" 는 보낼 수 있어야 한다. 전원이 답했는지로 보면 그 경우가 막힌다.

    셋이 교환할 때는 상대 둘이 다 넣어야 연다. 한 명만 넣은 상태에서는 여전히 기다리는 중이다.
  */
  const answered = appt.partners.length > 0 && appt.partners.every((p) => p.slots.length > 0)
  const overlap = appt.overlapSlot
  const matched = overlap !== null
  const confirmed = appt.confirmedLabel !== null
  // 겹치는 칸은 서버가 가장 빠른 하나만 알려준다. 밑줄은 그 자리에만 긋는다.
  const overlaps = matched ? [overlap] : []

  /**
   * 서버에 저장하고 결과로 화면을 맞춘다.
   *
   * 실패하면 서버에서 현재 상태를 다시 읽는다. 화면만 바뀐 채로 남으면 상대에게는 안 보이는 칸이
   * 나에게만 칠해져 있게 되고, 실패의 원인이 "상대가 먼저 했다" 인 경우에는 다시 읽는 것만으로
   * 화면이 맞는 상태가 된다.
   */
  const run = async (action: () => Promise<Exchange>) => {
    setBusy(true)
    try {
      const exchange = await action()
      dispatch({ type: 'exchange-synced', exchange, myUserId })
      return true
    } catch {
      const latest = await fetchExchange(appt.exchangeId).catch(() => null)
      if (latest) dispatch({ type: 'exchange-synced', exchange: latest, myUserId })
      return false
    } finally {
      setBusy(false)
    }
  }

  const toggle = (index: number) => {
    if (blocked.includes(index) || confirmed) return

    const next = appt.mySlots.includes(index)
      ? appt.mySlots.filter((i) => i !== index)
      : [...appt.mySlots, index].sort((a, b) => a - b)

    // 누른 즉시 칠한다. 서버 응답을 기다리면 손가락을 뗀 뒤에야 칸이 차서 눌린 느낌이 사라진다.
    dispatch({ type: 'my-slots-picked', slots: next })
    void run(() => updateTimeSlots(appt.exchangeId, myUserId, next)).then((ok) => {
      if (!ok) dispatch({ type: 'toast', message: '시간을 저장하지 못했어요' })
    })
  }

  /**
   * 약속을 확정한다.
   *
   * **상대가 먼저 눌렀으면 실패가 아니다.** 서버는 이미 정해진 약속을 다시 확정하지 못하게
   * 막는데, 누른 사람 입장에서는 원하던 일이 이미 일어난 것이라 그대로 약속 화면으로 넘어간다.
   */
  const confirmTime = async () => {
    if (confirmed) {
      navigate('/appointment')
      return
    }

    const ok = await run(() => confirmExchangeTime(appt.exchangeId, myUserId))

    if (ok || activeAppointment(state)?.confirmedLabel) {
      navigate('/appointment')
      return
    }

    dispatch({ type: 'toast', message: '잠시 후 다시 시도해주세요' })
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar onBack={() => navigate('/place')} onClose={() => setRejectOpen(true)} />

      <div className="flex-1 overflow-y-auto px-6 no-scrollbar">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">언제 만날까요?</h1>
        <p className="mt-3 text-[13px] leading-[1.6] text-neutral-400">
          가능한 시간을 모두 골라주세요
          <br />다 같이 만날 수 있는 가장 빠른 시간으로 정해져요
        </p>

        <div className="mt-8 overflow-x-auto no-scrollbar">
          <div className="min-w-[350px]">
            <div className="mb-1.5 grid grid-cols-[46px_repeat(8,1fr)] gap-1">
              <span />
              {slots.map((slot) => (
                <span
                  key={slot.index}
                  className={cn(
                    'text-center text-[10px]',
                    appt.mySlots.includes(slot.index) ? 'font-bold text-ink' : 'text-neutral-300',
                  )}
                >
                  {slot.label}
                </span>
              ))}
            </div>

            <TimeRow
              label="나"
              slots={appt.mySlots}
              color={MY_COLOR}
              overlaps={overlaps}
              blocked={blocked}
              interactive
              onToggle={toggle}
            />
            {appt.partners.map((partner) => (
              <TimeRow
                key={partner.userId}
                label={partner.name}
                slots={partner.slots}
                color={PARTNER_COLOR}
                overlaps={overlaps}
              />
            ))}

            {/* 모두가 되는 칸 아래에 밑줄을 그어 어디가 겹쳤는지 세로로 보이게 한다. */}
            <div className="grid grid-cols-[46px_repeat(8,1fr)] gap-1">
              <span />
              {Array.from({ length: SLOT_COUNT }, (_, i) => (
                <span
                  key={i}
                  className={cn(
                    'h-[3px] rounded-full',
                    overlaps.includes(i) ? 'bg-navy' : 'bg-transparent',
                  )}
                />
              ))}
            </div>
          </div>
        </div>

        <div className="mt-8 min-h-[92px]">
          <AnimatePresence mode="wait">
            {matched && (
              <motion.div
                key="matched"
                initial={{ opacity: 0, y: 12, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, scale: 0.98 }}
                transition={springSheet}
                className="flex items-center gap-3 rounded-2xl border border-brand bg-brand/10 p-4"
              >
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-white">
                  <ClockIcon className="size-5" />
                </span>
                <div>
                  <p className="text-[16px] font-bold text-ink">
                    {appt.confirmedLabel ?? slotTimeLabel(baseTime, overlap ?? 0)}에 만나요!
                  </p>
                  <p className="text-[12px] text-neutral-500">
                    다 같이 만날 수 있는 가장 빠른 시간
                  </p>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      <div className="shrink-0 px-6 pt-3 pb-8">
        {/*
          시안의 CTA 는 세 갈래다. 상대가 아직 시간을 안 넣었으면 아무것도 할 수 없고,
          넣었는데 겹치는 시간이 없으면 조율을 요청하고, 겹치면 그 자리에서 확정한다.
          내가 시간을 안 골랐어도 상대가 골랐으면 조율 요청은 보낼 수 있다.
        */}
        {!answered && <Button disabled>상대의 시간을 기다리는 중이에요</Button>}

        {answered && matched && (
          <Button variant="brand" disabled={busy} onClick={() => void confirmTime()}>
            {confirmed ? '약속 보러 가기' : '이 시간으로 약속!'}
          </Button>
        )}

        {answered && !matched && (
          <Button
            disabled={busy}
            onClick={() => {
              void run(() => resetTimeSlots(appt.exchangeId, myUserId)).then((ok) => {
                if (!ok) {
                  dispatch({ type: 'toast', message: '잠시 후 다시 시도해주세요' })
                  return
                }
                dispatch({ type: 'request-time-again' })
                navigate('/home')
              })
            }}
          >
            다른 시간 물어보기
          </Button>
        )}

        <TextButton onClick={() => navigate('/home')}>대기존 구경하며 기다리기</TextButton>
      </div>

      <RejectDialog
        open={rejectOpen}
        onKeep={() => setRejectOpen(false)}
        onReject={() => {
          setRejectOpen(false)
          void cancelAppointment().then((cancelled) => {
            if (cancelled) navigate('/home')
          })
        }}
      />
    </div>
  )
}

function TimeRow({
  label,
  slots,
  color,
  overlaps,
  blocked = [],
  interactive = false,
  onToggle,
}: {
  label: string
  slots: number[]
  color: string
  /** 모두가 되는 칸. 이 칸은 더 진한 남색으로 칠한다. */
  overlaps: number[]
  /** 다른 약속이 이미 잡은 칸 */
  blocked?: number[]
  interactive?: boolean
  onToggle?: (index: number) => void
}) {
  return (
    <div className="mb-1.5 grid grid-cols-[46px_repeat(8,1fr)] items-center gap-1">
      <span
        className={cn(
          'truncate pr-1 text-[11px] font-semibold',
          interactive ? 'text-ink' : 'text-neutral-400',
        )}
        title={label}
      >
        {label}
      </span>
      {Array.from({ length: SLOT_COUNT }, (_, i) => {
        const picked = slots.includes(i)
        const isBlocked = blocked.includes(i)
        const isOverlap = overlaps.includes(i) && picked
        const background = isBlocked
          ? BLOCKED_COLOR
          : isOverlap
            ? OVERLAP_COLOR
            : picked
              ? color
              : EMPTY_COLOR

        if (!interactive) {
          return (
            <motion.div
              key={i}
              animate={{ backgroundColor: background }}
              transition={springSnap}
              className="h-[30px] rounded-[7px]"
            />
          )
        }

        return (
          <motion.button
            key={i}
            type="button"
            onClick={() => {
              tick(6)
              onToggle?.(i)
            }}
            disabled={isBlocked}
            aria-pressed={picked}
            aria-label={`${label} ${i + 1}번째 시간`}
            whileTap={isBlocked ? undefined : { scale: 0.86 }}
            animate={{ backgroundColor: background }}
            transition={springSnap}
            // 내가 고르는 줄은 시안에서 상대 줄의 두 배 높이다. 손가락이 닿는 자리라 넓다.
            className="h-[64px] rounded-[9px]"
          />
        )
      })}
    </div>
  )
}

/**
 * 다른 약속의 확정 시각이 이 격자의 몇 번째 칸인지. 격자 밖이면 null 이다.
 *
 * 약속마다 격자 시작점이 다르기 때문에 칸 번호를 그대로 견줄 수 없다. 시각으로 되돌려 비교한다.
 */
function slotIndexOf(baseTime: Date, confirmedTime: string, slotCount: number): number | null {
  const minutes = (new Date(confirmedTime).getTime() - baseTime.getTime()) / 60_000
  const index = Math.round(minutes / 15)

  return index >= 0 && index < slotCount ? index : null
}
