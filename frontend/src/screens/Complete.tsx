import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { unknownItem, useItem } from '@/features/catalog/useItem'
import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import { pairsOf } from '@/store/matching'
import { activeAppointment } from '@/store/reducer'
import { useStore } from '@/store/useStore'

/**
 * 받은 카드 한 장.
 *
 * 한 장만 받았으면 크게 띄우고 설명까지 붙이는데, 여러 장이면 그 크기로는 두 장도 안 들어가서
 * 줄여 늘어놓고 이름만 남긴다.
 */
function ReceivedCard({ itemId, index, hero }: { itemId: number; index: number; hero: boolean }) {
  const item = useItem(itemId) ?? unknownItem(itemId)
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.7, rotate: -12 }}
      animate={{ opacity: 1, scale: 1, rotate: -4 }}
      // 한 장씩 차례로 떨어지게 해서 몇 장을 받았는지가 눈에 들어오게 한다.
      transition={{ ...springSnap, delay: 0.16 + index * 0.08 }}
      className={cn('text-center', hero ? 'w-[190px]' : 'w-[128px]')}
    >
      <div
        className={cn(
          'w-full bg-white',
          hero
            ? 'anim-float rounded-[22px] p-4 shadow-[0_16px_44px_rgba(0,0,0,0.16)]'
            : 'rounded-[18px] p-3 shadow-[0_10px_28px_rgba(0,0,0,0.12)]',
        )}
      >
        <div className={hero ? 'h-[190px]' : 'h-[122px]'}>
          <GoodsFace item={item} size="fill" />
        </div>
      </div>
      <p className={cn('font-extrabold text-ink', hero ? 'mt-8 text-[22px]' : 'mt-2 text-[14px]')}>
        {item.name}
      </p>
      {hero && item.description && (
        <p className="mt-1 text-[13px] text-neutral-400">{item.description}</p>
      )}
    </motion.div>
  )
}

/**
 * 교환 완료. 여기서 have/needs 에서 교환한 카드를 덜어내고,
 * Needs 가 남아 있으면 자동 매칭을 다시 켠다.
 */
export function Complete() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  // 정리 전에 무엇을 받았는지 붙잡아 둔다. dispatch 뒤에는 match 가 비기 때문이다.
  // 첫 쌍만 쓰면 여러 장을 바꿨을 때 나머지가 화면에서 사라지므로 쌍 전부를 센다.
  const [received] = useState(() => {
    const match = activeAppointment(state)?.match
    return match ? pairsOf(match).map((pair) => pair.receiveItemId) : []
  })

  useEffect(() => {
    dispatch({ type: 'complete' })
    // 화면에 들어온 순간 한 번만 정리한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const hero = received.length === 1

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex flex-1 flex-col items-center justify-center px-6">
        <motion.h1
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={springSnap}
          className="text-center text-[26px] font-extrabold tracking-[-0.02em] text-ink"
        >
          교환 완료! 🎉
        </motion.h1>

        {received.length > 1 && (
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ ...springSnap, delay: 0.1 }}
            className="mt-2 text-[14px] font-bold text-neutral-400"
          >
            카드 {received.length}장을 받았어요
          </motion.p>
        )}

        {received.length === 0 ? (
          // 무엇을 받았는지 모르는 경우다. 카드 자리는 지키되 앞면을 비운다.
          <motion.div
            initial={{ opacity: 0, scale: 0.7, rotate: -12 }}
            animate={{ opacity: 1, scale: 1, rotate: -4 }}
            transition={{ ...springSnap, delay: 0.16 }}
            className="mt-16"
          >
            <div className="anim-float w-[190px] rounded-[22px] bg-white p-4 shadow-[0_16px_44px_rgba(0,0,0,0.16)]">
              <div className="card-face h-[190px] w-full rounded-2xl" />
            </div>
          </motion.div>
        ) : (
          // 장수가 많으면 이 칸만 굴러간다. 화면 전체가 늘어나면 CTA 가 밀려 내려간다.
          <div
            className={cn(
              'w-full overflow-y-auto no-scrollbar',
              hero ? 'mt-16' : 'mt-10 max-h-[52vh]',
            )}
          >
            <div className="flex flex-wrap items-start justify-center gap-x-4 gap-y-6 pb-2">
              {received.map((itemId, i) => (
                <ReceivedCard key={`${itemId}-${i}`} itemId={itemId} index={i} hero={hero} />
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button variant="brand" onClick={() => navigate('/home')}>
          계속 교환하러 가기
        </Button>
        <TextButton
          onClick={() => {
            dispatch({ type: 'reset' })
            navigate('/')
          }}
        >
          교환 끝~
        </TextButton>
      </div>
    </div>
  )
}
