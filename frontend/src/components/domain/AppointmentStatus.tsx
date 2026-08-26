import { motion } from 'motion/react'

import { ClockIcon } from '@/components/ui/icons'
import { springSnap, staggerChild, staggerParent } from '@/lib/motion'
import type { AppointmentStatus } from '@/store/appointment-status'

/**
 * 진행 중인 약속들. 하나에 한 줄씩 세로로 쌓는다.
 *
 * 예전에는 가로로 밀어서 넘기는 칸이었는데, 약속이 둘 이상이면 두 번째부터는 밀어야만
 * 보였다. 약속은 시간이 정해져 있어서 놓치면 상대를 세워 두게 되는데, 화면 밖에 있으면
 * 있는 줄도 모른다. 세로로 쌓으면 몇 개가 잡혀 있는지가 한눈에 보인다.
 */
export function AppointmentStatusRail({
  statuses,
  onSelect,
}: {
  statuses: AppointmentStatus[]
  onSelect: (status: AppointmentStatus) => void
}) {
  return (
    <motion.div
      variants={staggerParent}
      initial="hidden"
      animate="show"
      className="flex flex-col gap-2"
    >
      {statuses.map((status) => (
        <motion.button
          key={status.id}
          type="button"
          onClick={() => onSelect(status)}
          variants={staggerChild}
          transition={springSnap}
          whileTap={{ scale: 0.98 }}
          className="flex w-full items-center gap-3 rounded-2xl border border-brand bg-brand/10 p-3 text-left"
        >
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand text-white">
            <ClockIcon className="size-5" />
          </span>
          <span className="min-w-0 flex-1">
            <span className="block truncate text-[17px] font-bold text-ink">{status.title}</span>
            <span className="block truncate text-[11px] font-medium text-neutral-500">
              {status.sub}
            </span>
          </span>
        </motion.button>
      ))}
    </motion.div>
  )
}
