import { AnimatePresence, motion, type PanInfo } from 'motion/react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { BottomSheet } from '@/components/domain/BottomSheet'
import { GoodsFace } from '@/components/domain/GoodsCard'
import { RadarRings } from '@/components/domain/Radar'
import { RadarUser } from '@/components/domain/RadarUser'
import { BellIcon, ClockIcon, SparkleIcon } from '@/components/ui/icons'
import { cn } from '@/lib/cn'
import { tick } from '@/lib/haptics'
import { springSheet, springSnap, staggerChild, staggerParent } from '@/lib/motion'
import { ALL_WAITING, itemById, ZONES } from '@/mocks/data'
import { radarUsers, sortedWaitingList } from '@/store/matching'
import { useStore } from '@/store/useStore'

export function Home() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const [sheetOpen, setSheetOpen] = useState(false)
  const [notifOpen, setNotifOpen] = useState(false)
  const [hovered, setHovered] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  // 방금 카드를 놓은 상대. 고리가 한 번 터지고 나서 찔러보기 확인 화면으로 넘어간다.
  const [burstOn, setBurstOn] = useState<string | null>(null)
  const radarRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    dispatch({ type: 'enter-home' })
    // 홈에 들어올 때 한 번만 자동 매칭을 켠다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 배열을 그대로 의존성에 넣으면 렌더마다 새 참조라 매번 다시 계산한다.
  // 문자열 하나로 눌러서 값이 실제로 바뀔 때만 돌게 한다.
  const needKey = state.needs.map((s) => s.itemId).join(',')
  const needIds = useMemo(() => (needKey ? needKey.split(',') : []), [needKey])
  const radar = useMemo(() => radarUsers(needIds), [needIds])
  const list = useMemo(() => sortedWaitingList(needIds), [needIds])

  const topItemId = state.have[0]?.itemId ?? 'sf'
  const haveCount = state.have.reduce((sum, s) => sum + s.qty, 0)
  const pendingTarget =
    state.outgoingPoke?.status === 'pending' ? state.outgoingPoke.targetUserId : null
  const appointment = state.appointment
  const zone = ZONES.find((z) => z.id === appointment?.zoneId)

  /** 끌고 있는 좌표로 어떤 상대 위에 있는지 찾는다. */
  const hitTest = (point: { x: number; y: number }): string | null => {
    const container = radarRef.current
    if (!container) return null
    const targets = container.querySelectorAll<HTMLElement>('[data-radar-user]')
    for (const el of targets) {
      const box = el.getBoundingClientRect()
      const pad = 18
      if (
        point.x >= box.left - pad &&
        point.x <= box.right + pad &&
        point.y >= box.top - pad &&
        point.y <= box.bottom + pad
      ) {
        return el.dataset.radarUser ?? null
      }
    }
    return null
  }

  const onDrag = (_: unknown, info: PanInfo) => {
    const found = hitTest(info.point)
    // 이미 답을 기다리는 상대에게는 다시 보낼 수 없다. 끌어놓는 동작 자체는 되지만
    // 강조 표시가 붙지 않아서 보낼 수 없다는 것이 손끝으로 전해진다.
    const next = found && found !== pendingTarget ? found : null
    // 대상에 처음 올라탄 순간에만 울린다. 매 프레임 울리면 손이 얼얼해진다.
    if (next && next !== hovered) tick(8)
    setHovered(next)
  }

  const onDragEnd = () => {
    setDragging(false)
    const target = hovered
    setHovered(null)
    if (!target) return
    tick([10, 40, 14])
    // 놓자마자 화면이 바뀌면 무슨 일이 일어났는지 안 보인다. 고리가 한 번 터지는
    // 동안만 붙잡아 두고 넘어간다.
    setBurstOn(target)
    window.setTimeout(() => {
      setBurstOn(null)
      navigate(`/poke/confirm?to=${target}`)
    }, 420)
  }

  const banner = (() => {
    if (appointment?.stage === 'confirmed') {
      return {
        tone: 'brand' as const,
        title: `${appointment.confirmedLabel} ${zone?.name ?? ''}`,
        body: `${itemById(state.match?.giveItemId ?? topItemId).name} 거래`,
        onClick: () => navigate('/appointment'),
      }
    }
    if (appointment) {
      return {
        tone: 'brand' as const,
        title:
          appointment.stage === 'time-conflict' ? '시간 조율 중이에요' : '약속을 잡는 중이에요',
        body: `${itemById(state.match?.giveItemId ?? topItemId).name} 거래`,
        onClick: () => navigate('/time'),
      }
    }
    if (state.match) {
      return {
        tone: 'white' as const,
        celebrate: state.match.origin === 'poke',
        title:
          state.match.origin === 'poke' ? '찔러보기가 성사됐어요!' : '서로의 니즈가 매칭됐어요!',
        body: '탭하여 확인',
        onClick: () => navigate('/match'),
      }
    }
    if (state.incomingPoke) {
      return {
        tone: 'white' as const,
        title: '상대가 교환을 요청했어요',
        body: '탭하여 확인',
        onClick: () => navigate('/poke/received'),
      }
    }
    return null
  })()

  /**
   * 레이더는 정사각형 무대 안에 그린다. 예전에는 세로로 늘어나는 칸에 백분율로 카드를
   * 붙였는데, 칸 높이가 화면마다 달라서 카드가 원에서 벗어나 서로 겹쳤다.
   * 이제 원 둘레를 각도로 나눠 앉히므로 어느 화면에서도 배치가 같다.
   */
  const radarPanel = (
    <div
      ref={radarRef}
      className="relative flex min-h-0 flex-1 flex-col items-center justify-center px-4 pt-6 pb-[96px] md:pt-2 md:pb-4"
    >
      <div className="relative aspect-square w-full max-w-[380px] shrink-0">
        <RadarRings />

        {radar.map((user, i) => {
          const angle = (-90 + i * (360 / Math.max(radar.length, 1))) * (Math.PI / 180)
          const radius = 38
          return (
            <div
              key={user.id}
              className="absolute -translate-x-1/2 -translate-y-1/2"
              style={{
                left: `${50 + radius * Math.cos(angle)}%`,
                top: `${50 + radius * Math.sin(angle)}%`,
              }}
            >
              <RadarUser
                user={user}
                index={i}
                hovered={hovered === user.id}
                pending={pendingTarget === user.id}
                burst={burstOn === user.id}
              />
            </div>
          )
        })}

        <div className="absolute top-1/2 left-1/2 z-20 -translate-x-1/2 -translate-y-1/2">
          <p className="mb-1 text-center text-[11px] font-bold text-neutral-500">내 카드</p>
          <motion.div
            drag
            dragSnapToOrigin
            dragMomentum={false}
            dragTransition={{ bounceStiffness: 480, bounceDamping: 30 }}
            transition={springSnap}
            onDragStart={() => setDragging(true)}
            onDrag={onDrag}
            onDragEnd={onDragEnd}
            whileDrag={{ zIndex: 60, cursor: 'grabbing' }}
            className="cursor-grab touch-none"
          >
            <CardStack topItemId={topItemId} count={Math.max(haveCount, 1)} lifted={dragging} />
          </motion.div>
        </div>
      </div>

      <p className="mt-5 shrink-0 text-center text-[12px] text-neutral-400">
        {dragging
          ? '놓아주면 찔러보기가 전송돼요'
          : '내 카드 묶음을 상대 카드 위에 끌어서 놓아보세요'}
      </p>
    </div>
  )

  const listPanel = (
    <motion.ul variants={staggerParent} initial="hidden" animate="show" className="space-y-2">
      {list.map((user) => {
        const item = itemById(user.itemId)
        const wanted = needIds.includes(user.itemId)
        return (
          <motion.li key={user.id} variants={staggerChild}>
            <motion.button
              type="button"
              onClick={() => navigate(`/poke/confirm?to=${user.id}`)}
              disabled={pendingTarget === user.id}
              whileTap={pendingTarget === user.id ? undefined : { scale: 0.97 }}
              transition={springSnap}
              className={cn(
                'flex w-full items-center gap-3 rounded-2xl border border-neutral-100 bg-white p-2.5 text-left',
                pendingTarget === user.id && 'opacity-45',
              )}
            >
              <div className="w-[52px] shrink-0">
                <GoodsFace item={item} size="sm" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-[14px] font-bold text-ink">{item.name}</p>
                <p className="truncate text-[12px] text-neutral-400">{user.nickname}</p>
              </div>
              {wanted && (
                <span className="rounded-full bg-brand/20 px-2.5 py-1 text-[11px] font-bold text-emerald-700">
                  찾는 굿즈
                </span>
              )}
              {pendingTarget === user.id && (
                <span className="text-[11px] font-semibold text-neutral-400">대기 중</span>
              )}
            </motion.button>
          </motion.li>
        )
      })}
    </motion.ul>
  )

  return (
    <div className="flex h-full flex-col">
      <header className="flex shrink-0 items-center justify-between px-5 pt-2">
        <h1 className="text-[23px] font-extrabold tracking-[-0.02em] text-ink">교환 대기장소</h1>
        <div className="flex items-center gap-1">
          <motion.button
            type="button"
            aria-label="알림"
            onClick={() => setNotifOpen(true)}
            whileTap={{ scale: 0.88 }}
            className="relative flex size-10 items-center justify-center text-ink"
          >
            <BellIcon className="size-[22px]" />
            {state.notifications.length > 0 && (
              <span className="absolute top-1.5 right-1.5 size-2 rounded-full bg-brand" />
            )}
          </motion.button>
        </div>
      </header>

      <div className="shrink-0 px-5 pt-2">
        <AnimatePresence mode="popLayout">
          {state.autoMatching && !banner && (
            <motion.span
              key="matching"
              initial={{ opacity: 0, y: -6, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={springSnap}
              className="inline-flex items-center gap-1.5 rounded-full bg-brand px-3.5 py-1.5 text-[12px] font-bold text-ink"
            >
              <span className="anim-blink size-1.5 rounded-full bg-ink" />
              자동 매칭 중
            </motion.span>
          )}

          {banner && (
            <motion.button
              key={banner.title}
              type="button"
              onClick={banner.onClick}
              initial={{ opacity: 0, y: -12, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8, scale: 0.98 }}
              transition={springSheet}
              whileTap={{ scale: 0.98 }}
              className={cn(
                'flex w-full items-center gap-3 rounded-2xl p-3.5 text-left',
                banner.tone === 'brand'
                  ? 'border border-brand bg-brand/10'
                  : 'bg-white shadow-[0_4px_18px_rgba(0,0,0,0.10)]',
              )}
            >
              <span
                className={cn(
                  'flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-ink',
                  'celebrate' in banner && banner.celebrate && 'anim-pop',
                )}
              >
                {banner.tone === 'brand' ? (
                  <ClockIcon className="size-5" />
                ) : (
                  <SparkleIcon className="size-5" />
                )}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-[15px] font-bold text-ink">
                  {banner.title}
                </span>
                <span className="block truncate text-[12px] text-neutral-500">{banner.body}</span>
              </span>
              <span className="text-[18px] text-neutral-300">›</span>
            </motion.button>
          )}
        </AnimatePresence>

        <AnimatePresence>
          {pendingTarget && (
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 8 }}
              transition={springSheet}
              className="mt-2 rounded-2xl bg-neutral-400/80 px-4 py-3 text-center"
            >
              <p className="text-[14px] font-bold text-white">
                {ALL_WAITING.find((u) => u.id === pendingTarget)?.nickname}님에게 교환을 제안했어요
              </p>
              <p className="text-[12px] text-white/75">답변 기다리는 중</p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/*
        레이더는 한 번만 그린다. 예전에는 모바일용과 데스크톱용으로 두 벌을 그렸는데,
        같은 JSX 에 붙은 ref 가 나중에 마운트된 (화면에 없는) 쪽을 가리키는 바람에
        끌어놓기 충돌 판정이 항상 빗나갔다.
      */}
      <div className="relative flex min-h-0 flex-1 flex-col overflow-hidden md:flex-row md:gap-7 md:px-7 md:pb-7">
        <div className="relative min-h-0 flex-1 md:rounded-3xl md:bg-neutral-50/70">
          {radarPanel}
        </div>

        <aside className="hidden w-[340px] shrink-0 flex-col md:flex">
          <div className="flex items-baseline justify-between px-1 pb-3">
            <h2 className="text-[17px] font-extrabold text-ink">전체리스트</h2>
            <span className="text-[12px] text-neutral-400">전체 {list.length}개</span>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto pr-1 pb-2 no-scrollbar">{listPanel}</div>
        </aside>

        <div className="md:hidden">
          <BottomSheet
            open={sheetOpen}
            onOpenChange={setSheetOpen}
            peek={72}
            height={460}
            header={
              <div className="flex items-baseline justify-between">
                <span className="text-[17px] font-extrabold text-ink">전체리스트</span>
                <span className="text-[12px] text-neutral-400">전체 {list.length}개</span>
              </div>
            }
          >
            {listPanel}
          </BottomSheet>
        </div>
      </div>

      <NotificationSheet
        open={notifOpen}
        onClose={() => setNotifOpen(false)}
        notifications={state.notifications}
        onSelect={(kind) => {
          setNotifOpen(false)
          if (kind === 'poke-received') navigate('/poke/received')
          else if (kind === 'match' || kind === 'poke-accepted') navigate('/match')
          else if (kind === 'time-request') navigate('/time')
        }}
      />
    </div>
  )
}

/**
 * 알림. 모바일에서는 아래에서 올라오는 시트, 데스크톱에서는 종 아래에 붙는 판이다.
 * 넓은 화면에서 바텀시트가 올라오는 것은 데스크톱 앱에서 쓰지 않는 방식이라 어색하다.
 */
function NotificationSheet({
  open,
  onClose,
  notifications,
  onSelect,
}: {
  open: boolean
  onClose: () => void
  notifications: { id: string; kind: string; title: string; body: string }[]
  onSelect: (kind: string) => void
}) {
  const body =
    notifications.length === 0 ? (
      <p className="py-10 text-center text-[13px] text-neutral-400">아직 알림이 없어요</p>
    ) : (
      <ul className="mt-3 space-y-2 overflow-y-auto no-scrollbar">
        {notifications.map((n) => (
          <li key={n.id}>
            <motion.button
              type="button"
              onClick={() => onSelect(n.kind)}
              whileTap={{ scale: 0.97 }}
              transition={springSnap}
              className="w-full rounded-2xl bg-neutral-50 p-3.5 text-left"
            >
              <p className="text-[14px] font-bold text-ink">{n.title}</p>
              <p className="text-[12px] text-neutral-400">{n.body}</p>
            </motion.button>
          </li>
        ))}
      </ul>
    )

  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.button
            type="button"
            aria-label="알림 닫기"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 z-40 bg-black/30 md:bg-black/10"
          />

          {/* 모바일: 바텀시트 */}
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={springSheet}
            className="absolute inset-x-0 bottom-0 z-50 max-h-[62%] rounded-t-[26px] bg-white p-5 shadow-2xl md:hidden"
          >
            <span aria-hidden className="mx-auto mb-4 block h-1 w-9 rounded-full bg-neutral-200" />
            <h2 className="text-[18px] font-extrabold text-ink">알림</h2>
            {body}
            <motion.button
              type="button"
              onClick={onClose}
              whileTap={{ scale: 0.97 }}
              transition={springSnap}
              className="mt-4 w-full rounded-full border border-neutral-200 py-3 text-[14px] font-semibold text-neutral-500"
            >
              닫기
            </motion.button>
          </motion.div>

          {/* 데스크톱: 종 아래에 붙는 판 */}
          <motion.div
            initial={{ opacity: 0, y: -10, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={springSheet}
            className="absolute top-16 right-7 z-50 hidden max-h-[70%] w-[340px] origin-top-right flex-col overflow-hidden rounded-2xl bg-white p-5 shadow-[0_18px_50px_rgba(0,0,0,0.18)] md:flex"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-[17px] font-extrabold text-ink">알림</h2>
              <motion.button
                type="button"
                aria-label="닫기"
                onClick={onClose}
                whileTap={{ scale: 0.85 }}
                transition={springSnap}
                className="text-[20px] font-light text-neutral-400"
              >
                ✕
              </motion.button>
            </div>
            <div className="min-h-0 flex-1 overflow-y-auto no-scrollbar">{body}</div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
