/**
 * 시안에 들어 있는 iOS 상태 표시줄이다. 실제 시각을 쓰면 스크린샷과 어긋나므로
 * 시안 그대로 9:41 로 둔다. 데스크톱에서는 숨긴다.
 */
export function StatusBar() {
  return (
    <div className="flex h-11 shrink-0 items-center justify-between px-6 text-[13px] font-semibold text-ink md:hidden">
      <span>9:41</span>
      <span className="flex items-center gap-1.5 text-[11px]">
        <span className="tracking-[-2px]">●●●</span> Wi-Fi <span>▬</span>
      </span>
    </div>
  )
}
