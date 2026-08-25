import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { PinIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { zonePinAt } from '@/mocks/data'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { useStore } from '@/store/useStore'

/**
 * 교환 장소 확인. 지정 장소는 하나로 정해져 있고, 이 화면은 그 위치가 행사장
 * 어디쯤인지 눈으로 보여주는 자리다. 그래서 다른 핀은 고를 수 없다.
 *
 * 장소의 이름과 위치는 서버에서 온다. 약도와 핀 자리는 아직 목업이라 화면이 정하고,
 * 서버가 준 목록 순서대로 좌표를 얹는다.
 */
export function PlaceSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const cancelAppointment = useCancelAppointment()
  const [cancelOpen, setCancelOpen] = useState(false)

  const appt = useLastDefined(state.appointment)
  const zones = state.zones

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

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar onBack={() => navigate('/match')} onClose={() => setCancelOpen(true)} />

      <div className="flex-1 overflow-y-auto px-6 no-scrollbar">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">
          교환 장소를 확인해주세요
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">핀 위치에서 교환할 수 있어요</p>

        <div className="relative mt-6 h-[230px] overflow-hidden rounded-2xl bg-neutral-100">
          {/* 운영측에서 받은 약도 자리. 지금은 격자로 대신한다. */}
          <div
            aria-hidden
            className="absolute inset-0"
            style={{
              backgroundImage:
                'linear-gradient(#0000000f 1px, transparent 1px), linear-gradient(90deg, #0000000f 1px, transparent 1px)',
              backgroundSize: '25% 33.33%',
            }}
          />
          <div className="absolute top-[46%] left-[36%] flex h-[32%] w-[30%] items-end justify-center rounded-lg bg-white/70 pb-2">
            <p className="text-[10px] text-neutral-300">팝업 매장</p>
          </div>

          {zones.map((zone, index) => {
            const pin = zonePinAt(index)
            const isHere = zone.id === appt.zone.id

            return (
              <motion.button
                type="button"
                key={zone.id}
                onClick={() =>
                  dispatch({
                    type: 'toast',
                    message: isHere
                      ? `${appt.zone.name}에서 교환해요`
                      : `이번 행사는 ${appt.zone.name}에서만 교환할 수 있어요`,
                  })
                }
                initial={{ opacity: 0, y: -8, scale: 0.7 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                whileTap={{ scale: 0.88 }}
                transition={springSnap}
                className="absolute -translate-x-1/2 -translate-y-1/2 text-center"
                style={{ left: `${pin.x}%`, top: `${pin.y}%` }}
              >
                <span
                  className={
                    isHere
                      ? 'flex size-7 items-center justify-center rounded-full bg-ink text-white'
                      : 'flex size-6 items-center justify-center rounded-full bg-neutral-300 text-white'
                  }
                >
                  <PinIcon className={isHere ? 'size-4' : 'size-3.5'} />
                </span>
                <span className="mt-1 block text-[10px] text-neutral-400">{zone.name}</span>
              </motion.button>
            )
          })}
        </div>

        <motion.button
          type="button"
          onClick={() =>
            dispatch({ type: 'toast', message: `${appt.zone.name}으로 정해져 있어요` })
          }
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          whileTap={{ scale: 0.98 }}
          transition={{ ...springSnap, delay: 0.1 }}
          className="mt-5 flex w-full items-center gap-3 rounded-2xl border-2 border-ink bg-neutral-50 p-3.5 text-left"
        >
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-ink">
            <PinIcon className="size-5" />
          </span>
          <div>
            <p className="text-[15px] font-bold text-ink">{appt.zone.name}</p>
            <p className="text-[12px] text-neutral-400">{appt.zone.location}</p>
          </div>
          <span className="ml-auto flex size-6 items-center justify-center rounded-full bg-ink text-[12px] text-white">
            ✓
          </span>
        </motion.button>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button onClick={() => navigate('/time')}>시간 선택하기</Button>
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
