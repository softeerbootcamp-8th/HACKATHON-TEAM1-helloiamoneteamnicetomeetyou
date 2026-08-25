import { motion } from 'motion/react'

/** 레이더 배경. 거리 표시가 아니라 상대들이 둘러 서 있다는 것을 보여주는 장치다. */
export function RadarRings() {
  return (
    <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
      {[1, 0.72, 0.46].map((scale, i) => (
        <motion.span
          key={scale}
          className="absolute aspect-square w-[86%] rounded-full border border-neutral-200/70"
          style={{ scale }}
          animate={{ opacity: [0.35, 0.75, 0.35] }}
          transition={{ duration: 3.4, repeat: Infinity, delay: i * 0.5, ease: 'easeInOut' }}
        />
      ))}
      <span className="sweep-glow absolute aspect-square w-[86%] rounded-full" />
    </div>
  )
}
