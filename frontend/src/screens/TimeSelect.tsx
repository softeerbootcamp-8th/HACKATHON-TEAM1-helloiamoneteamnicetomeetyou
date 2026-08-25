import { AnimatePresence, motion } from 'motion/react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { Button, TextButton } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { ClockIcon } from '@/components/ui/icons'
import { StatusBar } from '@/components/ui/StatusBar'
import { TopBar } from '@/components/ui/TopBar'
import { cn } from '@/lib/cn'
import { springSheet, springSnap } from '@/lib/motion'
import { buildSlots, earliestOverlap, slotTimeLabel, SLOT_COUNT } from '@/store/time'
import { useStore } from '@/store/useStore'

/** 상대별 색. 시안이 사람마다 다른 색으로 칠해 둔다. */
const ROW_COLORS = ['bg-brand', 'bg-violet-300', 'bg-yellow-300']

export function TimeSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const [cancelOpen, setCancelOpen] = useState(false)
  // 시각은 화면에 들어온 순간으로 고정한다. 매 렌더마다 다시 계산하면 칸이 밀린다.
  const [now] = useState(() => new Date())
  const slots = useMemo(() => buildSlots(now), [now])

  const appt = state.appointment
  const match = state.match

  if (!appt || !match) {
    return (
      <div className="flex h-full flex-col">
        <StatusBar />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="text-[15px] text-neutral-500">진행 중인 약속이 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  const partners = match.kind === 'ONE_TO_ONE' ? [match.partner] : [match.giver, match.receiver]

  const answered = Object.keys(appt.partnerSlots).length > 0
  const rows = [appt.mySlots, ...partners.map((p) => appt.partnerSlots[p.id] ?? [])]
  const overlap = answered ? earliestOverlap(rows) : -1
  const matched = overlap !== -1
  const conflict = answered && !matched

  const toggle = (index: number) => {
    const next = appt.mySlots.includes(index)
      ? appt.mySlots.filter((i) => i !== index)
      : [...appt.mySlots, index].sort((a, b) => a - b)
    dispatch({ type: 'set-my-slots', slots: next })
  }

  return (
    <div className="flex h-full flex-col">
      <StatusBar />
      <TopBar onBack={() => navigate('/place')} onClose={() => setCancelOpen(true)} />

      <div className="flex-1 overflow-y-auto px-6 no-scrollbar">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">언제 만날까요?</h1>
        <p className="mt-3 text-[13px] leading-[1.6] text-neutral-400">
          가능한 시간을 모두 눌러주세요.
          <br />
          모두 겹치는 가장 빠른 시간으로 정해져요.
        </p>

        <span className="mt-6 inline-flex items-center gap-1.5 rounded-full bg-neutral-100 px-3.5 py-1.5 text-[12px] font-semibold text-neutral-600">
          <span className="size-1.5 rounded-full bg-ink" />
          {partners.map((p) => p.nickname).join(', ')}님과 매칭
        </span>

        <div className="mt-4 overflow-x-auto no-scrollbar">
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
              color={ROW_COLORS[0]}
              overlap={overlap}
              interactive
              onToggle={toggle}
            />
            {partners.map((p, i) => (
              <TimeRow
                key={p.id}
                label={p.nickname}
                slots={appt.partnerSlots[p.id] ?? []}
                color={ROW_COLORS[(i + 1) % ROW_COLORS.length]}
                overlap={overlap}
              />
            ))}
          </div>
        </div>

        <p className="mt-4 text-[12px] text-neutral-400">
          오늘 지금부터 2시간까지만 고를 수 있어요.
        </p>

        <div className="mt-5 min-h-[92px]">
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
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-ink">
                  <ClockIcon className="size-5" />
                </span>
                <div>
                  <p className="text-[16px] font-bold text-ink">
                    {slotTimeLabel(slots, overlap, now)}에 만나요
                  </p>
                  <p className="text-[12px] text-neutral-500">모두 되는 가장 빠른 시간</p>
                </div>
              </motion.div>
            )}

            {conflict && (
              <motion.p
                key="conflict"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                transition={springSnap}
                className="py-6 text-center text-[15px] font-semibold text-neutral-400"
              >
                맞는 시간이 없어요
              </motion.p>
            )}
          </AnimatePresence>
        </div>
      </div>

      <div className="shrink-0 px-6 pt-3 pb-8">
        {matched && (
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

        {conflict && (
          <Button
            onClick={() => {
              dispatch({ type: 'request-time-again' })
              navigate('/home')
            }}
          >
            시간 조율 요청하기
          </Button>
        )}

        {!answered && (
          <Button disabled>
            {appt.mySlots.length === 0 ? '가능한 시간을 골라주세요' : '아직 상대방을 기다려야 해요'}
          </Button>
        )}

        <TextButton onClick={() => navigate('/home')}>
          {answered ? '홈으로' : '홈으로 가서 기다리기'}
        </TextButton>
      </div>

      <Dialog
        open={cancelOpen}
        title="거래를 취소할까요?"
        cancelLabel="아니요"
        confirmLabel="취소할게요"
        onCancel={() => setCancelOpen(false)}
        onConfirm={() => {
          setCancelOpen(false)
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
  overlap,
  interactive = false,
  onToggle,
}: {
  label: string
  slots: number[]
  color: string
  overlap: number
  interactive?: boolean
  onToggle?: (index: number) => void
}) {
  return (
    <div className="mb-1.5 grid grid-cols-[46px_repeat(8,1fr)] items-center gap-1">
      <span className="truncate pr-1 text-[11px] font-semibold text-neutral-500" title={label}>
        {label}
      </span>
      {Array.from({ length: SLOT_COUNT }, (_, i) => {
        const picked = slots.includes(i)
        const isOverlap = overlap === i
        const Tag = interactive ? motion.button : motion.div
        return (
          <Tag
            key={i}
            {...(interactive
              ? {
                  type: 'button' as const,
                  onClick: () => onToggle?.(i),
                  whileTap: { scale: 0.86 },
                  'aria-pressed': picked,
                  'aria-label': `${label} ${i + 1}번째 시간`,
                }
              : {})}
            layout
            transition={springSnap}
            className={cn(
              'h-[30px] rounded-[7px]',
              isOverlap && picked ? 'bg-ink' : picked ? color : 'bg-neutral-100',
            )}
          />
        )
      })}
    </div>
  )
}
