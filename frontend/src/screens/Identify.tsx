import { AnimatePresence, motion } from 'motion/react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { BreakupDialog } from '@/components/domain/ConfirmDialogs'
import { completeExchange } from '@/lib/exchange'
import { tick } from '@/lib/haptics'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { getDeviceId } from '@/store/identity'
import { identityLabel, identityMarkAt } from '@/store/identity-mark'
import { activeAppointment } from '@/store/reducer'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { useStore } from '@/store/useStore'

/**
 * 서로를 찾는 화면. 같은 그림을 든 사람이 상대다.
 * 다른 화면과 달리 어두운 배경이라 상태 표시줄도 흰색으로 둔다.
 */
export function Identify() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const cancelAppointment = useCancelAppointment()
  const [noShowOpen, setNoShowOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  // 레몬을 누른 횟수. 누를 때마다 키가 바뀌어서 흔들림이 처음부터 다시 돈다.
  const [pokes, setPokes] = useState(0)

  const myUserId = useMemo(() => getDeviceId(), [])
  const appt = useLastDefined(activeAppointment(state))
  const active = activeAppointment(state)

  /*
    식별자는 교환 하나에 하나이고 참가자 전원이 같은 값을 든다. 그래서 같은 화면을 든 사람이
    곧 내 교환 상대다. 진행 중인 다른 교환과 겹치지 않게 서버가 골라 준다.

    약속 없이 주소로 바로 들어온 경우에는 첫 표시로 보여준다. 화면이 비면 무엇을 보는 자리인지
    알 수 없기 때문이다.
  */
  const mark = identityMarkAt(appt?.identityMark ?? 0)
  const label = appt ? identityLabel(appt.identityMark, appt.identityNumber) : mark.name

  /*
    상대가 먼저 "만났어요" 를 눌렀을 때 내 화면도 따라간다. 서버가 EXCHANGE_COMPLETED 를 보내면
    약속 단계가 완료로 바뀌고, 그걸 보고 넘어간다.

    두 사람이 서로 다른 버튼을 누를 수 있어서 필요하다. 한 명이 만났다고 하고 다른 한 명이
    "상대가 오지 않아요" 를 누르면, 먼저 도착한 쪽만 반영되고 늦은 쪽은 그 결과를 따라야 한다.
  */
  useEffect(() => {
    if (active?.stage === 'completed') navigate('/complete')
  }, [active?.stage, navigate])

  /**
   * "만났어요". 서버에 끝났다고 남긴 다음 완료 화면으로 간다.
   *
   * 상대가 먼저 취소했으면 실패한다. 그때는 서버가 알려 준 결과를 그대로 받아들인다.
   */
  const goComplete = async () => {
    if (!appt) {
      navigate('/complete')
      return
    }

    setBusy(true)
    try {
      const exchange = await completeExchange(appt.exchangeId, myUserId)
      dispatch({ type: 'exchange-synced', exchange, myUserId })
      navigate('/complete')
    } catch {
      dispatch({ type: 'toast', message: '상대가 먼저 거래를 취소했어요' })
      navigate('/home')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="relative flex h-full flex-col text-white">
      {/*
        배경은 화면을 다 덮는다. 판이 노치 밑에 주는 여백까지 끌어올리지 않으면
        어두운 화면 위쪽에 흰 띠가 남는다.
      */}
      <span
        aria-hidden
        className="pointer-events-none absolute inset-x-0 -top-[max(0.75rem,env(safe-area-inset-top))] bottom-0"
        style={{
          background: 'linear-gradient(160deg, #3dd2ff8c 0%, #0a1a33 35%, #050d1c 100%), #050d1c',
        }}
      />

      {/* 내용 폭만 다른 화면과 같이 맞춘다. */}
      <div className="relative mx-auto flex h-full w-full flex-col md:max-w-[900px] md:px-10">
        <div className="flex h-14 shrink-0 items-center justify-end px-4">
          <motion.button
            type="button"
            aria-label="닫기"
            onClick={() => navigate('/home')}
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
              aria-label={`${mark.name} 흔들기`}
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
                key={`${mark.src}-${pokes}`}
                src={mark.src}
                alt=""
                aria-hidden
                animate={pokes > 0 ? { rotate: [0, -9, 7, -4, 0], scale: [1, 1.08, 0.98, 1] } : {}}
                transition={{ duration: 0.7, ease: 'easeOut' }}
                className="anim-fruit w-[260px] max-w-[70vw] select-none"
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
            disabled={busy}
            onClick={() => void goComplete()}
            className="h-[54px] w-full rounded-full bg-white text-[16px] font-bold text-ink disabled:opacity-60"
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

      <BreakupDialog
        open={noShowOpen}
        onKeep={() => setNoShowOpen(false)}
        onFindNew={() => {
          setNoShowOpen(false)
          void cancelAppointment().then((cancelled) => {
            if (cancelled) navigate('/home')
          })
        }}
      />
    </div>
  )
}
