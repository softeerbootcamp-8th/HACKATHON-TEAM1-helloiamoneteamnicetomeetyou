/**
 * 레이더 배경. 거리 표시가 아니라 상대들이 둘러 서 있다는 것을 보여주는 장치다.
 *
 * 정원이 아니라 타원이다. 세로로 긴 화면에서 정원으로 그리면 폭에 막혀 아래쪽이
 * 통째로 비고 카드끼리도 붙어 보인다.
 */
export function RadarRings() {
  return (
    <div className="pointer-events-none absolute inset-0">
      {[1, 0.72, 0.46].map((scale, i) => (
        <span
          key={scale}
          className="anim-ring absolute top-1/2 left-1/2 h-[84%] w-[88%] -translate-x-1/2 -translate-y-1/2 rounded-[50%] border border-neutral-200/70"
          style={{ scale, animationDelay: `${i * 0.5}s` }}
        />
      ))}
      {/* 빛무리가 한 방향으로 계속 돈다. 레이더가 훑고 있다는 느낌을 주는 자리다. */}
      <span className="anim-sweep sweep-glow absolute top-1/2 left-1/2 h-[84%] w-[88%] -translate-x-1/2 -translate-y-1/2 rounded-[50%]" />
    </div>
  )
}
