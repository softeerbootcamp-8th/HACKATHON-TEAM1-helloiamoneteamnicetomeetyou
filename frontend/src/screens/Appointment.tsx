import { motion } from 'motion/react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { PinIcon } from '@/components/ui/icons'
import { cn } from '@/lib/cn'
import { arriveAtExchange } from '@/lib/exchange'
import { springSnap } from '@/lib/motion'
import { itemById } from '@/mocks/data'
import { useLastDefined } from '@/lib/useLastDefined'
import { getDeviceId } from '@/store/identity'
import { useStore } from '@/store/useStore'

/** 확정된 약속. 시간과 장소, 주고받을 카드를 다시 보여준다. */
export function Appointment() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const appt = useLastDefined(state.appointment)
  const match = useLastDefined(state.match)
  const myUserId = useMemo(() => getDeviceId(), [])
  const [busy, setBusy] = useState(false)

  // 남은 시간을 1분마다 다시 센다. 화면을 열어 둔 채로 시간이 흘러도 숫자가 따라간다.
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000)
    return () => window.clearInterval(timer)
  }, [])

  if (!appt || !match || (appt.stage !== 'confirmed' && appt.stage !== 'arrived')) {
    return (
      <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="text-[15px] text-neutral-500">확정된 약속이 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  const isThreeWay = match.kind === 'THREE_WAY'
  const headline = countdownLabel(appt.confirmedTime, now)

  const goArrive = async () => {
    setBusy(true)
    try {
      const exchange = await arriveAtExchange(appt.exchangeId, myUserId)
      dispatch({ type: 'exchange-synced', exchange, myUserId })
      navigate('/identify')
    } catch {
      dispatch({ type: 'toast', message: '잠시 후 다시 시도해주세요' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex-1 overflow-y-auto px-6 pt-8 no-scrollbar">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={springSnap}
          className="text-[26px] font-extrabold tracking-[-0.02em] text-ink"
        >
          {headline}
        </motion.h1>
        <p className="mt-2 text-[13px] text-neutral-400">약속정보</p>

        <motion.div
          initial={{ opacity: 0, y: 14, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ ...springSnap, delay: 0.06 }}
          className="mt-3 flex items-center gap-3 rounded-2xl border-2 border-ink p-3.5"
        >
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-ink">
            <PinIcon className="size-5" />
          </span>
          <div>
            <p className="text-[16px] font-bold text-ink">
              {appt.confirmedLabel} {appt.zone.name}
            </p>
            <p className="text-[12px] text-neutral-400">{appt.zone.location}</p>
          </div>
        </motion.div>

        <div className="mt-10 flex items-start justify-center gap-4">
          <ArrivalCard itemId={match.giveItemId} caption="내가 줄 것" arrived={appt.myArrived} />
          <span className="anim-breathe mt-16 text-[20px] text-ink">⇄</span>
          <ArrivalCard
            itemId={match.receiveItemId}
            caption="내가 받을 것"
            arrived={appt.partners.every((p) => p.arrived)}
          />
        </div>

        {/* 상대가 여럿이면 누가 왔는지 한 줄로 보여준다. 배지만으로는 셋 중 누구인지 알 수 없다. */}
        {appt.partners.length > 0 && (
          <p className="mt-8 text-center text-[12px] text-neutral-400">
            {appt.partners.map((p) => `${p.name} ${p.arrived ? '도착' : '이동중'}`).join(' · ')}
            {isThreeWay && ' · 셋이서 교환해요'}
          </p>
        )}
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button disabled={busy} onClick={() => void goArrive()}>
          {appt.myArrived ? '상대 찾으러 가기' : '도착했어요'}
        </Button>
        <TextButton onClick={() => navigate('/home')}>홈으로</TextButton>
      </div>
    </div>
  )
}

/**
 * "15분 뒤 만나요!" 자리. 서버가 확정해 준 시각에서 실제로 남은 시간을 센다.
 *
 * 시안이 15분으로 그려져 있는데 그건 예시라, 고정 문구로 두면 30분 뒤 약속에도 15분이라고 나온다.
 * 약속 시각이 지난 뒤에도 화면이 열려 있을 수 있어서 그 경우를 따로 둔다.
 */
function countdownLabel(confirmedTime: string | null, now: number): string {
  if (!confirmedTime) return '곧 만나요!'

  const minutes = Math.round((new Date(confirmedTime).getTime() - now) / 60_000)

  if (minutes <= 0) return '지금 만나요!'
  if (minutes < 60) return `${minutes}분 뒤 만나요!`

  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours}시간 뒤 만나요!` : `${hours}시간 ${rest}분 뒤 만나요!`
}

function ArrivalCard({
  itemId,
  caption,
  arrived,
}: {
  itemId: string
  caption: string
  arrived: boolean
}) {
  const item = itemById(itemId)
  return (
    <div className="text-center">
      <motion.span
        animate={{ backgroundColor: arrived ? '#111111' : '#d4d4d4' }}
        transition={springSnap}
        className={cn('mb-3 inline-block rounded-full px-3 py-1 text-[11px] font-bold text-white')}
      >
        {arrived ? '도착' : '이동중'}
      </motion.span>
      <div className="w-[118px] rounded-2xl bg-white p-3 shadow-[0_6px_22px_rgba(0,0,0,0.10)]">
        <GoodsFace item={item} size="lg" />
        <p className="mt-2.5 text-center text-[12px] font-bold text-ink">{item.name}</p>
      </div>
      <p className="mt-2.5 text-[12px] text-neutral-500">{caption}</p>
    </div>
  )
}
