import { AnimatePresence, motion } from 'motion/react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { Button, TextButton } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { ClockIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { cn } from '@/lib/cn'
import { confirmExchangeTime, resetTimeSlots, updateTimeSlots, type Exchange } from '@/lib/exchange'
import { tick } from '@/lib/haptics'
import { springSheet, springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { getDeviceId } from '@/store/identity'
import { buildSlots, parseSlotBaseTime, slotTimeLabel } from '@/store/time'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { useStore } from '@/store/useStore'

/**
 * 상대별 색. 시안이 사람마다 다른 색으로 칠해 둔다.
 * 클래스 대신 값으로 두는 이유는 색이 바뀔 때 툭 튀지 않고 이어지게 하기 위해서다.
 */
const ROW_COLORS = ['#2ced90', '#c4b5fd', '#fde047']
const EMPTY_COLOR = '#f5f5f5'
const OVERLAP_COLOR = '#111111'

export function TimeSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const cancelAppointment = useCancelAppointment()
  const [cancelOpen, setCancelOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const myUserId = useMemo(() => getDeviceId(), [])

  const appt = useLastDefined(state.appointment)

  const slots = useMemo(
    () => (appt ? buildSlots(parseSlotBaseTime(appt.slotBaseTime)) : []),
    [appt],
  )

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

  const matched = appt.overlapSlot !== null
  const conflict = appt.allAnswered && !matched
  const waiting = !appt.allAnswered

  /**
   * 서버에 저장하고 결과로 화면을 맞춘다.
   *
   * 실패하면 마지막으로 성공한 상태를 다시 읽어 되돌린다. 화면만 바뀐 채로 남으면
   * 상대에게는 안 보이는 칸이 나에게만 칠해져 있게 된다.
   */
  const run = async (action: () => Promise<Exchange>) => {
    setBusy(true)
    try {
      const exchange = await action()
      dispatch({ type: 'exchange-synced', exchange, myUserId })
      return true
    } catch {
      dispatch({ type: 'toast', message: '잠시 후 다시 시도해주세요' })
      return false
    } finally {
      setBusy(false)
    }
  }

  const toggle = (index: number) => {
    const next = appt.mySlots.includes(index)
      ? appt.mySlots.filter((i) => i !== index)
      : [...appt.mySlots, index].sort((a, b) => a - b)

    // 누른 즉시 칠한다. 서버 응답을 기다리면 손가락을 뗀 뒤에야 칸이 차서 눌린 느낌이 사라진다.
    dispatch({ type: 'my-slots-picked', slots: next })
    void run(() => updateTimeSlots(appt.exchangeId, myUserId, next))
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
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
          {appt.partners.map((p) => p.name).join(', ')}님과 매칭
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
              slotCount={appt.slotCount}
              slots={appt.mySlots}
              color={ROW_COLORS[0]}
              overlap={appt.overlapSlot}
              interactive
              onToggle={toggle}
            />
            {appt.partners.map((partner, i) => (
              <TimeRow
                key={partner.userId}
                label={partner.name}
                slotCount={appt.slotCount}
                slots={partner.slots}
                color={ROW_COLORS[(i + 1) % ROW_COLORS.length]}
                overlap={appt.overlapSlot}
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
                    {appt.confirmedLabel ??
                      slotTimeLabel(parseSlotBaseTime(appt.slotBaseTime), appt.overlapSlot ?? 0)}
                    에 만나요
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
            disabled={busy}
            onClick={() => {
              void run(() => confirmExchangeTime(appt.exchangeId, myUserId)).then((ok) => {
                if (ok) navigate('/appointment')
              })
            }}
          >
            약속 확정하기
          </Button>
        )}

        {conflict && (
          <Button
            disabled={busy}
            onClick={() => {
              void run(() => resetTimeSlots(appt.exchangeId, myUserId)).then((ok) => {
                if (!ok) return
                dispatch({ type: 'request-time-again' })
                navigate('/home')
              })
            }}
          >
            시간 조율 요청하기
          </Button>
        )}

        {waiting && (
          <Button disabled>
            {appt.mySlots.length === 0 ? '가능한 시간을 골라주세요' : '아직 상대방을 기다려야 해요'}
          </Button>
        )}

        <TextButton onClick={() => navigate('/home')}>
          {waiting ? '홈으로 가서 기다리기' : '홈으로'}
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
          void cancelAppointment()
          navigate('/home')
        }}
      />
    </div>
  )
}

function TimeRow({
  label,
  slotCount,
  slots,
  color,
  overlap,
  interactive = false,
  onToggle,
}: {
  label: string
  slotCount: number
  slots: number[]
  color: string
  overlap: number | null
  interactive?: boolean
  onToggle?: (index: number) => void
}) {
  return (
    <div className="mb-1.5 grid grid-cols-[46px_repeat(8,1fr)] items-center gap-1">
      <span className="truncate pr-1 text-[11px] font-semibold text-neutral-500" title={label}>
        {label}
      </span>
      {Array.from({ length: slotCount }, (_, i) => {
        const picked = slots.includes(i)
        const isOverlap = overlap === i && picked
        const background = isOverlap ? OVERLAP_COLOR : picked ? color : EMPTY_COLOR

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
            aria-pressed={picked}
            aria-label={`${label} ${i + 1}번째 시간`}
            whileTap={{ scale: 0.86 }}
            animate={{ backgroundColor: background }}
            transition={springSnap}
            className="h-[30px] rounded-[7px]"
          />
        )
      })}
    </div>
  )
}
