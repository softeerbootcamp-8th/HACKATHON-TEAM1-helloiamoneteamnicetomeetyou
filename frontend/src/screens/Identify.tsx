import { AnimatePresence, motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { Dialog } from '@/components/ui/Dialog'
import { tick } from '@/lib/haptics'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { identityLabel, identityMarkAt } from '@/store/identity-mark'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { useStore } from '@/store/useStore'

/**
 * 서로를 찾는 화면. 같은 그림을 든 사람이 상대다.
 * 다른 화면과 달리 어두운 배경이라 상태 표시줄도 흰색으로 둔다.
 */
export function Identify() {
  const navigate = useNavigate()
  const { state } = useStore()
  const cancelAppointment = useCancelAppointment()
  const appt = useLastDefined(state.appointment)
  const [noShowOpen, setNoShowOpen] = useState(false)
  // 레몬을 누른 횟수. 누를 때마다 키가 바뀌어서 흔들림이 처음부터 다시 돈다.
  const [pokes, setPokes] = useState(0)

  /*
    식별자는 교환 하나에 하나이고 참가자 전원이 같은 값을 든다. 그래서 같은 화면을 든 사람이
    곧 내 교환 상대다. 진행 중인 다른 교환과 겹치지 않게 서버가 골라 준다.

    약속 없이 주소로 바로 들어온 경우에는 첫 표시로 보여준다. 화면이 비면 무엇을 보는 자리인지
    알 수 없기 때문이다.
  */
  const mark = identityMarkAt(appt?.identityMark ?? 0)
  const label = appt ? identityLabel(appt.identityMark, appt.identityNumber) : mark.name

  return (
    <div
      className="relative flex h-full flex-col text-white"
      style={{
        background: 'linear-gradient(160deg, #3dd2ff8c 0%, #0a1a33 35%, #050d1c 100%), #050d1c',
      }}
    >
      {/* 배경은 화면을 다 덮고, 내용 폭만 다른 화면과 같이 맞춘다. */}
      <div className="mx-auto flex h-full w-full flex-col md:max-w-[900px] md:px-10">
        <div className="flex h-14 shrink-0 items-center justify-end px-4">
          <motion.button
            type="button"
            aria-label="닫기"
            onClick={() => navigate('/appointment')}
            whileTap={{ scale: 0.88 }}
            className="flex size-10 items-center justify-center text-[26px] font-light text-white/80"
          >
            ✕
          </motion.button>
        </div>

        <div className="flex flex-1 flex-col items-center justify-center px-8">
          <motion.div
            initial={{ scale: 0.7, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ ...springSnap, delay: 0.05 }}
            className="relative"
          >
            {/*
              누르면 반응한다. 상대를 찾는 동안 화면을 들고 서 있는 시간이 길어서,
              만질 거리가 하나 있는 편이 낫다.
            */}
            <motion.button
              type="button"
              aria-label="레몬 흔들기"
              onClick={() => {
                tick(12)
                setPokes((n) => n + 1)
              }}
              whileTap={{ scale: 0.9 }}
              transition={springSnap}
              className="relative block"
            >
              <AnimatePresence>
                {pokes > 0 && (
                  <motion.span
                    key={pokes}
                    aria-hidden
                    initial={{ opacity: 0.7, scale: 0.5 }}
                    animate={{ opacity: 0, scale: 1.9 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 1.1, ease: [0.25, 0.6, 0.3, 1] }}
                    className="pointer-events-none absolute top-1/2 left-1/2 aspect-square w-[70%] -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-[#d6ff4b]"
                  />
                )}
              </AnimatePresence>

              <motion.img
                key={pokes}
                src="/lemon.svg"
                alt=""
                aria-hidden
                animate={pokes > 0 ? { rotate: [0, -9, 7, -4, 0], scale: [1, 1.08, 0.98, 1] } : {}}
                transition={{ duration: 0.7, ease: 'easeOut' }}
                className="anim-lemon w-[260px] max-w-[70vw] select-none"
                style={{ filter: `hue-rotate(${mark.hueRotate}deg)` }}
                draggable={false}
              />
            </motion.button>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...springSnap, delay: 0.15 }}
            className="mt-12 text-[28px] font-extrabold"
          >
            {label}
          </motion.h1>
          <p className="mt-3 text-center text-[14px] leading-[1.55] text-white/70">
            같은 화면을 든 사람이
            <br />내 교환 상대예요.
          </p>
          <p className="mt-4 text-[13px] text-white/45">휴대폰 화면을 들어 서로를 찾아보세요.</p>
        </div>

        <div className="shrink-0 space-y-2.5 px-6 pb-8">
          <motion.button
            type="button"
            whileTap={{ scale: 0.97 }}
            onClick={() => navigate('/complete')}
            className="h-[54px] w-full rounded-full bg-white text-[16px] font-bold text-ink"
          >
            만났어요
          </motion.button>
          <motion.button
            type="button"
            whileTap={{ scale: 0.97 }}
            onClick={() => setNoShowOpen(true)}
            className="h-[54px] w-full rounded-full bg-black/55 text-[16px] font-bold text-white"
          >
            상대가 오지 않아요
          </motion.button>
        </div>
      </div>

      <Dialog
        open={noShowOpen}
        title="거래를 취소할까요?"
        description="상대가 오지 않으면 약속을 접고 다시 상대를 찾습니다."
        cancelLabel="조금 더 기다릴게요"
        confirmLabel="취소할게요"
        onCancel={() => setNoShowOpen(false)}
        onConfirm={() => {
          setNoShowOpen(false)
          void cancelAppointment()
          navigate('/home')
        }}
      />
    </div>
  )
}
