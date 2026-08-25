import { motion } from 'motion/react'

import type { PushState } from '@/lib/use-push'
import { springSnap } from '@/lib/motion'

/**
 * 알림 목록 위에 붙는 한 줄짜리 안내다.
 *
 * 알림을 켜지 않으면 앱을 닫은 사이의 소식을 놓치는데, 그걸 알려 줄 자리가 알림 목록보다
 * 자연스러운 곳이 없다. 켜고 나면 사라진다.
 */
export function PushOptInBanner({ state, onEnable }: { state: PushState; onEnable: () => void }) {
  // 켜져 있거나, 이 브라우저로는 안 되거나, 아직 확인 중이면 자리를 차지하지 않는다.
  if (state.status === 'enabled' || state.status === 'unsupported' || state.status === 'loading') {
    return null
  }

  if (state.status === 'needs-install') {
    return (
      <Card>
        <p className="text-[13px] font-bold text-ink">홈 화면에 추가하면 알림을 받을 수 있어요</p>
        <p className="mt-1 text-[12px] leading-relaxed text-neutral-400">
          아래 공유 버튼을 누르고 <b className="text-neutral-500">홈 화면에 추가</b>를 고른 다음, 홈
          화면의 아이콘으로 다시 들어와 주세요.
        </p>
      </Card>
    )
  }

  if (state.status === 'denied') {
    return (
      <Card>
        <p className="text-[13px] font-bold text-ink">알림이 꺼져 있어요</p>
        <p className="mt-1 text-[12px] leading-relaxed text-neutral-400">
          설정 앱 &gt; 알림 &gt; NearLy 에서 켜주세요.
        </p>
      </Card>
    )
  }

  if (state.status === 'error') {
    return (
      <Card>
        <p className="text-[13px] font-bold text-ink">{state.reason}</p>
      </Card>
    )
  }

  const enabling = state.status === 'enabling'

  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-[13px] font-bold text-ink">알림 받기</p>
          <p className="mt-0.5 text-[12px] text-neutral-400">
            앱을 닫아 두어도 매칭 소식을 보내드려요
          </p>
        </div>

        <motion.button
          type="button"
          onClick={onEnable}
          disabled={enabling}
          whileTap={{ scale: 0.96 }}
          transition={springSnap}
          className="shrink-0 rounded-full bg-ink px-4 py-2 text-[13px] font-semibold text-white disabled:opacity-50"
        >
          {enabling ? '켜는 중' : '켜기'}
        </motion.button>
      </div>
    </Card>
  )
}

function Card({ children }: { children: React.ReactNode }) {
  return <div className="mt-3 rounded-2xl bg-neutral-50 px-4 py-3">{children}</div>
}
