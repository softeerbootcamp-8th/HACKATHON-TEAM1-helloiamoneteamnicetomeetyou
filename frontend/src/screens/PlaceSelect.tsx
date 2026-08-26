import { motion } from 'motion/react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { RejectDialog } from '@/components/domain/ConfirmDialogs'
import { Button } from '@/components/ui/Button'
import { PinIcon } from '@/components/ui/icons'
import { TopBar } from '@/components/ui/TopBar'
import { updateExchangeZone } from '@/lib/exchange'
import { messageOf } from '@/lib/api'
import { springSnap } from '@/lib/motion'
import { getDeviceId } from '@/store/identity'
import { activeAppointment } from '@/store/reducer'
import { useCancelAppointment } from '@/store/use-cancel-appointment'
import { useLastDefined } from '@/lib/useLastDefined'
import { useStore } from '@/store/useStore'

/**
 * 만날 자리를 고른다.
 *
 * 구역은 어드민에서 만들고 고치고 지운다. 이름과 위치, 약도 위 자리까지 전부 서버에서 오기
 * 때문에 운영이 자리를 늘려도 화면을 고칠 일이 없다. 약도 이미지만 아직 없어서 화면이 격자로
 * 대신 그리고, 그 위에 서버가 준 비율대로 핀을 찍는다.
 *
 * **고른 자리는 서버에 저장되고 상대 화면에도 곧바로 반영된다.** 한쪽만 옮기면 다른 한 명이
 * 옛 자리에서 기다리게 되는데, 그게 이 화면에서 제일 나쁜 결과다.
 */
export function PlaceSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const [rejectOpen, setRejectOpen] = useState(false)
  const cancelAppointment = useCancelAppointment()
  const appt = useLastDefined(activeAppointment(state))

  // 약속 없이 주소로 바로 들어온 경우에는 시간 화면과 같은 자리로 보낸다.
  const here = appt?.zone ?? null
  const zones = state.zones
  const [moving, setMoving] = useState(false)
  const myUserId = useMemo(() => getDeviceId(), [])

  /**
   * 핀을 눌러 자리를 옮긴다.
   *
   * 이미 그 자리면 서버를 부르지 않는다. 같은 값을 저장해 봐야 상대에게 "자리가 바뀌었어요"
   * 알림만 한 번 더 가고 화면은 그대로다.
   *
   * <b>응답으로 화면을 맞춘다.</b> 서버는 자리를 바꾼 본인에게는 일부러 실시간 알림을 보내지
   * 않는다(`ExchangeService.updateZone`). 방금 자기가 한 행동이 자기 알림함에 쌓이기 때문인데,
   * 그래서 누른 사람의 화면을 갱신할 길이 이 응답뿐이다. 전에는 이걸 버려서 핀을 눌러도
   * 서버에만 저장되고 아래 자리 카드와 진한 핀은 옛 자리에 그대로 있었다. 상대 화면은
   * 알림을 받아 옮겨졌기 때문에, 정작 자리를 옮긴 사람만 다른 곳을 보고 있었다.
   */
  const moveTo = async (zoneId: number) => {
    if (!appt || moving || zoneId === here?.id) return

    setMoving(true)
    try {
      const exchange = await updateExchangeZone(appt.exchangeId, myUserId, zoneId)
      dispatch({ type: 'exchange-synced', exchange, myUserId })
    } catch (error) {
      dispatch({ type: 'toast', message: messageOf(error) })
    } finally {
      setMoving(false)
    }
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <TopBar onBack={() => navigate('/home')} onClose={() => setRejectOpen(true)} />

      <div className="flex-1 overflow-y-auto px-6 no-scrollbar">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">어디서 만날까요?</h1>
        <p className="mt-2 text-[13px] text-neutral-400">핀을 누르면 만날 자리가 바뀌어요</p>

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

          {zones.map((zone) => (
            <motion.button
              type="button"
              key={zone.id}
              onClick={() => void moveTo(zone.id)}
              disabled={moving}
              aria-current={zone.id === here?.id}
              initial={{ opacity: 0, y: -8, scale: 0.7 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              whileTap={{ scale: 0.88 }}
              transition={springSnap}
              className="absolute -translate-x-1/2 -translate-y-1/2 text-center"
              style={{ left: `${zone.mapX}%`, top: `${zone.mapY}%` }}
            >
              {/*
                고를 수 있는 핀이 흐리면 눌러 봐야 안 된다고 읽힌다. 지금 자리를 진하게 두고
                나머지는 그보다 한 단계만 연하게 둬서, 둘 다 누를 수 있는 것으로 보이게 한다.
              */}
              <PinIcon
                className={
                  zone.id === here?.id
                    ? 'mx-auto size-8 text-brand'
                    : 'mx-auto size-7 text-neutral-400'
                }
              />
              <span
                className={
                  zone.id === here?.id
                    ? 'mt-1 block text-[10px] font-bold text-ink'
                    : 'mt-1 block text-[10px] text-neutral-400'
                }
              >
                {zone.name}
              </span>
            </motion.button>
          ))}
        </div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springSnap, delay: 0.1 }}
          className="mt-5 flex w-full items-center gap-3 rounded-2xl border-2 border-ink bg-neutral-50 p-3.5 text-left"
        >
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-white">
            <PinIcon className="size-5" />
          </span>
          <div>
            <p className="text-[15px] font-bold text-ink">
              {here?.name ?? '만날 자리를 불러오는 중'}
            </p>
            <p className="text-[12px] text-neutral-400">{here?.location ?? ''}</p>
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
