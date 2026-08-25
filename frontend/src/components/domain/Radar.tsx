/** 레이더 배경. 거리 표시가 아니라 상대들이 둘러 서 있다는 것을 보여주는 장치다. */
export function RadarRings() {
  return (
    <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
      {[1, 0.72, 0.46].map((scale, i) => (
        <span
          key={scale}
          className="anim-ring absolute aspect-square w-[86%] rounded-full border border-neutral-200/70"
          style={{ scale, animationDelay: `${i * 0.5}s` }}
        />
      ))}
      {/* 빛무리가 한 방향으로 계속 돈다. 레이더가 훑고 있다는 느낌을 주는 자리다. */}
      <span className="anim-sweep sweep-glow absolute aspect-square w-[86%] rounded-full" />
    </div>
  )
}
