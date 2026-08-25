import { motion, type PanInfo } from 'motion/react'

import { tick } from '@/lib/haptics'

/**
 * 왼쪽 가장자리에서 오른쪽으로 밀면 뒤로 간다. 토스를 포함해 웬만한 앱이 다 되는 동작이라
 * 없으면 손이 먼저 어색해한다.
 *
 * 가장자리 24px 만 잡는다. 레이더의 카드 끌기나 바텀시트와 겹치지 않게 하기 위해서다.
 */
export function SwipeBackEdge({ onBack }: { onBack: () => void }) {
  const handleDragEnd = (_: unknown, info: PanInfo) => {
    const farEnough = info.offset.x > 70
    const fastEnough = info.velocity.x > 420
    if (farEnough || fastEnough) {
      tick(10)
      onBack()
    }
  }

  return (
    <motion.div
      aria-hidden
      drag="x"
      dragConstraints={{ left: 0, right: 0 }}
      dragElastic={{ left: 0, right: 0.6 }}
      dragMomentum={false}
      onDragEnd={handleDragEnd}
      className="absolute inset-y-0 left-0 z-40 w-6 touch-none"
    />
  )
}
