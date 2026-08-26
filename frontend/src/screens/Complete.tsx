import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'

import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { springSnap } from '@/lib/motion'
import { useItem } from '@/features/catalog/useItem'
import { activeAppointment } from '@/store/reducer'
import { useStore } from '@/store/useStore'

/**
 * 교환 완료. 여기서 have/needs 에서 교환한 카드를 덜어내고,
 * Needs 가 남아 있으면 자동 매칭을 다시 켠다.
 */
export function Complete() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  // 정리 전에 무엇을 받았는지 붙잡아 둔다. dispatch 뒤에는 match 가 비기 때문이다.
  const [received] = useState(() => activeAppointment(state)?.match?.receiveItemId ?? null)

  useEffect(() => {
    dispatch({ type: 'complete' })
    // 화면에 들어온 순간 한 번만 정리한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const item = useItem(received) ?? null

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
        <motion.div
          initial={{ opacity: 0, scale: 0.7, rotate: -12 }}
          animate={{ opacity: 1, scale: 1, rotate: -4 }}
          transition={{ ...springSnap, delay: 0.16 }}
          className="mt-16"
        >
          <div className="anim-float w-[190px] rounded-[22px] bg-white p-4 shadow-[0_16px_44px_rgba(0,0,0,0.16)]">
            {item ? (
              <div className="h-[190px]">
                <GoodsFace item={item} size="fill" />
              </div>
            ) : (
              <div className="card-face h-[190px] w-full rounded-2xl" />
            )}
          </div>
        </motion.div>

        {item && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...springSnap, delay: 0.3 }}
            className="mt-12 text-center"
          >
            <p className="text-[22px] font-extrabold text-ink">{item.name}</p>
            <p className="mt-1 text-[13px] text-neutral-400">{item.description}</p>
          </motion.div>
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
