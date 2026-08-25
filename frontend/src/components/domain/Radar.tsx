/**
 * 레이더 배경. 거리 표시가 아니라 상대들이 둘러 서 있다는 것을 보여주는 장치다.
 *
 * 시안이 정원이라 여기도 정원으로 그린다. 부모가 정사각형 무대라서 원이 찌그러지지 않는다.
 * 지름은 시안 비율(340 / 250 / 158)을 그대로 옮겼다.
 */
export function RadarRings() {
  return (
    <div className="pointer-events-none absolute inset-0">
      {[1, 0.735, 0.465].map((scale, i) => (
        <span
          key={scale}
          className="anim-ring absolute top-1/2 left-1/2 aspect-square w-full -translate-x-1/2 -translate-y-1/2 rounded-full border border-neutral-200/70"
          style={{ scale, animationDelay: `${i * 0.5}s` }}
        />
      ))}
      {/*
        부채꼴이 한 방향으로 훑고 지나간다. 시안의 Shape / .sweep 자리다.
        시안에서 스윕은 링보다 조금 커서(422 대 340) 링 밖으로 살짝 번진다.
      */}
      <span className="sweep-fan absolute top-1/2 left-1/2 aspect-square w-[124%] -translate-x-1/2 -translate-y-1/2 rounded-full" />
    </div>
  )
}
