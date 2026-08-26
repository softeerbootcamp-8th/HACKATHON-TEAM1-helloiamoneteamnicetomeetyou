import { motion } from 'motion/react'

import { ClockIcon } from '@/components/ui/icons'
import { springSnap } from '@/lib/motion'
import type { AppointmentStatus } from '@/store/appointment-status'

/**
 * 약속이 둘 이상이면 가로로 밀어서 다음 약속을 본다. 한 번에 하나씩만 넘어가도록
 * 스크롤 스냅을 칸 단위로 걸어 둔다.
 */
export function AppointmentStatusRail({
  statuses,
  onSelect,
}: {
  statuses: AppointmentStatus[]
  onSelect: (status: AppointmentStatus) => void
}) {
  const single = statuses.length === 1

  return (
    /* 오른쪽만 화면 밖으로 흘려서 다음 약속이 살짝 걸쳐 보이게 한다. */
    <div className="-mr-5 flex snap-x snap-mandatory gap-3 overflow-x-auto pr-5 pb-1 no-scrollbar md:mr-0 md:pr-0">
      {statuses.map((status) => (
        <motion.button
          key={status.id}
          type="button"
          onClick={() => onSelect(status)}
          initial={{ opacity: 0, y: -8, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={springSnap}
          whileTap={{ scale: 0.98 }}
          className={
            'flex shrink-0 snap-start items-center gap-3 rounded-2xl border border-brand bg-brand/10 p-3 text-left ' +
            (single ? 'w-full' : 'w-[86%] md:w-full')
          }
        >
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand text-white">
            <ClockIcon className="size-5" />
          </span>
          <span className="min-w-0">
            <span className="block truncate text-[17px] font-bold text-ink">{status.title}</span>
            <span className="block truncate text-[11px] font-medium text-neutral-500">
              {status.sub}
            </span>
          </span>
        </motion.button>
      ))}
    </div>
  )
}
