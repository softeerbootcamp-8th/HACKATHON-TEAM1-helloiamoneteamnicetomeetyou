import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { Dialog } from '@/components/ui/Dialog'
import { springSnap } from '@/lib/motion'
import { MY_IDENTITY } from '@/mocks/data'
import { useStore } from '@/store/useStore'

/**
 * 서로를 찾는 화면. 같은 그림을 든 사람이 상대다.
 * 다른 화면과 달리 어두운 배경이라 상태 표시줄도 흰색으로 둔다.
 */
export function Identify() {
  const navigate = useNavigate()
  const { dispatch } = useStore()
  const [noShowOpen, setNoShowOpen] = useState(false)

  return (
    <div
      className="relative flex h-full flex-col text-white"
      style={{
        background: 'linear-gradient(160deg, #3dd2ff8c 0%, #0a1a33 35%, #050d1c 100%), #050d1c',
      }}
    >
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
          {/* Figma 시안의 레몬을 그대로 내보낸 것이다. 빛무리도 이 안에 들어 있다. */}
          <img
            src="/lemon.svg"
            alt=""
            aria-hidden
            className="anim-lemon w-[260px] max-w-[70vw] select-none"
            draggable={false}
          />
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springSnap, delay: 0.15 }}
          className="mt-12 text-[28px] font-extrabold"
        >
          {MY_IDENTITY.fruit} {MY_IDENTITY.number}
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

      <Dialog
        open={noShowOpen}
        title="거래를 취소할까요?"
        description="상대가 오지 않으면 약속을 접고 다시 상대를 찾습니다."
        cancelLabel="조금 더 기다릴게요"
        confirmLabel="취소할게요"
        onCancel={() => setNoShowOpen(false)}
        onConfirm={() => {
          setNoShowOpen(false)
          dispatch({ type: 'cancel-appointment' })
          navigate('/home')
        }}
      />
    </div>
  )
}
