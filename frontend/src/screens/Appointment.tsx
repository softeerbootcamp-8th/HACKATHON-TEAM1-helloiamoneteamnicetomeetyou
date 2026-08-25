import { motion } from 'motion/react'
import { useNavigate } from 'react-router'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { PinIcon } from '@/components/ui/icons'
import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import { itemById, ZONES } from '@/mocks/data'
import { useStore } from '@/store/useStore'

/** 확정된 약속. 시간과 장소, 주고받을 카드를 다시 보여준다. */
export function Appointment() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const appt = state.appointment
  const match = state.match

  if (!appt || !match || appt.stage === 'place') {
    return (
      <div className="flex h-full flex-col">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="text-[15px] text-neutral-500">확정된 약속이 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  const zone = ZONES.find((z) => z.id === appt.zoneId)
  const isThreeWay = match.kind === 'THREE_WAY'

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto px-6 pt-8 no-scrollbar">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={springSnap}
          className="text-[26px] font-extrabold tracking-[-0.02em] text-ink"
        >
          15분 뒤 만나요!
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
              {appt.confirmedLabel} {zone?.name}
            </p>
            <p className="text-[12px] text-neutral-400">{zone?.location}</p>
          </div>
        </motion.div>

        <div className="mt-10 flex items-start justify-center gap-4">
          <ArrivalCard
            itemId={match.giveItemId}
            caption="내가 줄 것"
            status="이동중"
            arrived={appt.stage === 'arrived'}
          />
          <span className="anim-breathe mt-16 text-[20px] text-ink">⇄</span>
          <ArrivalCard itemId={match.receiveItemId} caption="내가 받을 것" status="도착" arrived />
        </div>

        {isThreeWay && (
          <p className="mt-8 text-center text-[12px] text-neutral-400">
            {match.giver.nickname}, {match.receiver.nickname}님과 셋이서 교환해요
          </p>
        )}
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button
          onClick={() => {
            dispatch({ type: 'arrive' })
            navigate('/identify')
          }}
        >
          도착했어요
        </Button>
        <TextButton onClick={() => navigate('/home')}>홈으로</TextButton>
      </div>
    </div>
  )
}

function ArrivalCard({
  itemId,
  caption,
  status,
  arrived,
}: {
  itemId: string
  caption: string
  status: string
  arrived: boolean
}) {
  const item = itemById(itemId)
  return (
    <div className="text-center">
      <span
        className={cn(
          'mb-3 inline-block rounded-full px-3 py-1 text-[11px] font-bold',
          arrived ? 'bg-ink text-white' : 'bg-neutral-300 text-white',
        )}
      >
        {status}
      </span>
      <div className="w-[118px] rounded-2xl bg-white p-3 shadow-[0_6px_22px_rgba(0,0,0,0.10)]">
        <GoodsFace item={item} size="lg" />
        <p className="mt-2.5 text-center text-[12px] font-bold text-ink">{item.name}</p>
      </div>
      <p className="mt-2.5 text-[12px] text-neutral-500">{caption}</p>
    </div>
  )
}
