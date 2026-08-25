import { AnimatePresence, motion } from 'motion/react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { RejectDialog } from '@/components/domain/ConfirmDialogs'
import { Button, TextButton } from '@/components/ui/Button'
import { ClockIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { cn } from '@/lib/cn'
import { tick } from '@/lib/haptics'
import { springSheet, springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { activeAppointment } from '@/store/reducer'
import {
  buildSlots,
  earliestOverlap,
  overlappingSlots,
  slotTimeLabel,
  SLOT_COUNT,
} from '@/store/time'
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
  // 시각은 화면에 들어온 순간으로 고정한다. 매 렌더마다 다시 계산하면 칸이 밀린다.
  const [now] = useState(() => new Date())
  const slots = useMemo(() => buildSlots(now), [now])

  const appt = useLastDefined(activeAppointment(state))

  if (!appt) {
    return (
      <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="text-[15px] text-neutral-500">진행 중인 약속이 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  const { match } = appt
  const partners = match.kind === 'ONE_TO_ONE' ? [match.partner] : [match.giver, match.receiver]

  /**
   * 이미 확정한 다른 약속이 차지한 칸. 같은 시간에 두 군데를 갈 수 없으니 못 고르게 막는다.
   * 시안의 `11_v2` 자리다.
   */
  const blocked = state.appointments
    .filter((other) => other.id !== appt.id && other.confirmedSlot !== null)
    .map((other) => other.confirmedSlot as number)

  const answered = Object.keys(appt.partnerSlots).length > 0
  const rows = [appt.mySlots, ...partners.map((p) => appt.partnerSlots[p.id] ?? [])]
  const overlap = answered ? earliestOverlap(rows) : -1
  const overlaps = answered ? overlappingSlots(rows) : []
  const matched = overlap !== -1

  const toggle = (index: number) => {
    if (blocked.includes(index)) return
    const next = appt.mySlots.includes(index)
      ? appt.mySlots.filter((i) => i !== index)
      : [...appt.mySlots, index].sort((a, b) => a - b)
    dispatch({ type: 'set-my-slots', slots: next })
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar onBack={() => navigate('/place')} onClose={() => setRejectOpen(true)} />

      <div className="flex-1 overflow-y-auto px-6 no-scrollbar">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">언제 만날까요?</h1>
        <p className="mt-3 text-[13px] leading-[1.6] text-neutral-400">
          가능한 시간을 모두 눌러주세요.
          <br />
          모두 겹치는 가장 빠른 시간으로 정해져요.
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
            {partners.map((p) => (
              <TimeRow
                key={p.id}
                label={p.nickname}
                slots={appt.partnerSlots[p.id] ?? []}
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
                    {slotTimeLabel(slots, overlap, now)}에 만나요
                  </p>
                  <p className="text-[12px] text-neutral-500">모두가 만날 수 있는 가장 빠른 시간</p>
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
        {!answered && <Button disabled>아직 상대방을 기다려야 해요</Button>}

        {answered && matched && (
          <Button
            variant="brand"
            onClick={() => {
              dispatch({
                type: 'confirm-time',
                slot: overlap,
                label: slotTimeLabel(slots, overlap, now),
              })
              navigate('/appointment')
            }}
          >
            약속 확정하기
          </Button>
        )}

        {answered && !matched && (
          <Button
            onClick={() => {
              dispatch({ type: 'request-time-again' })
              navigate('/home')
            }}
          >
            시간 조율 요청하기
          </Button>
        )}

        <TextButton onClick={() => navigate('/home')}>홈으로 가서 기다리기</TextButton>
      </div>

      <RejectDialog
        open={rejectOpen}
        onKeep={() => setRejectOpen(false)}
        onReject={() => {
          setRejectOpen(false)
          dispatch({ type: 'cancel-appointment' })
          navigate('/home')
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
