import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { RejectDialog } from '@/components/domain/ConfirmDialogs'
import { EmptyState } from '@/components/domain/EmptyState'
import { Button } from '@/components/ui/Button'
import { PinIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { activeAppointment } from '@/store/reducer'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { useStore } from '@/store/useStore'

/**
 * 만날 장소를 확인한다. 시안의 `10. 장소 보기` 다.
 *
 * **고르는 화면이 아니다.** 교환 장소는 팝업 운영자가 미리 정해 둔 한 곳이고, 서버가 교환을
 * 만들 때 그 자리를 붙여 준다. 이 화면은 그 자리를 약도 위에 찍어 보여주기만 한다.
 *
 * 예전에는 참가자가 핀을 눌러 자리를 옮길 수 있었다. 한쪽만 옮기면 다른 한 명이 옛 자리에서
 * 기다리는 일이 생기고, 실제 행사에서는 교환 자리를 운영이 통제해야 해서 뺐다. 자리를 옮기는
 * 길은 이제 어드민 콘솔뿐이고, 그때는 참가자 전원에게 실시간으로 전해진다.
 */
export function PlaceView() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const [rejectOpen, setRejectOpen] = useState(false)
  const cancelAppointment = useCancelAppointment()
  const appt = useLastDefined(activeAppointment(state))

  if (!appt) {
    return (
      <EmptyState
        title="진행 중인 약속이 없어요"
        description={'교환이 성사되면\n만날 장소를 알려드릴게요.'}
        icon={<PinIcon className="size-9" />}
        onAction={() => navigate('/home')}
      />
    )
  }

  const zone = appt.zone

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar onBack={() => navigate('/home')} onClose={() => setRejectOpen(true)} />

      <div className="flex-1 overflow-y-auto px-6 no-scrollbar">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">
          만날 장소를 확인해주세요
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">핀이 있는 곳에서 만날 수 있어요</p>

        {/*
          약도는 보기만 하는 그림이다. 누를 것이 없다는 것을 알려 주려고 눌리면 토스트를 띄운다.
          아무 반응이 없으면 "안 눌리는 화면" 이 아니라 "고장난 화면" 으로 읽힌다.

          문구는 시안의 `토스트 정리`(204:5620) 를 따른다. 자리 이름을 박지 않고 서버가 준
          값을 끼우는 것은, 운영이 어드민에서 자리를 옮기면 토스트도 같이 따라가야 해서다.
        */}
        <div
          onClick={() =>
            dispatch({ type: 'toast', message: `이번엔 ${zone.name}에서만 만날 수 있어요` })
          }
          className="relative mt-6 h-[252px] overflow-hidden rounded-[18px] border border-line bg-neutral-50"
        >
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
          <div className="absolute top-[55.6%] left-[37.1%] flex h-[35.7%] w-[28.6%] items-end justify-center rounded-lg bg-white/70 pb-2">
            <p className="text-[10px] text-neutral-300">팝업 매장</p>
          </div>

          {/*
            지정 교환장소 핀 하나. 자리는 서버가 준 약도 위 백분율이라, 운영이 자리를 옮겨도
            화면을 고칠 일이 없다.
          */}
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.7 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={springSnap}
            className="absolute -translate-x-1/2 -translate-y-1/2 text-center"
            style={{ left: `${zone.mapX}%`, top: `${zone.mapY}%` }}
          >
            <span className="mx-auto flex size-[34px] items-center justify-center rounded-full border-2 border-white bg-ink text-white shadow-[0_2px_8px_rgba(0,0,0,0.12)]">
              <PinIcon className="size-[18px]" />
            </span>
            <span className="mt-1 block text-[10px] font-bold text-ink">{zone.name}</span>
          </motion.div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springSnap, delay: 0.1 }}
          className="mt-5 flex w-full items-center gap-3.5 rounded-[18px] border-[1.5px] border-ink bg-neutral-50 px-4 py-3.5 text-left"
        >
          <span className="flex size-[38px] shrink-0 items-center justify-center rounded-xl bg-brand text-white">
            <PinIcon className="size-5" />
          </span>
          <div>
            <p className="text-[17px] font-bold text-ink">{zone.name}</p>
            <p className="text-[11px] text-neutral-400">{zone.location}</p>
          </div>
        </motion.div>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button onClick={() => navigate('/time')}>시간 정하러 가기</Button>
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
