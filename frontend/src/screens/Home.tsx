import { AnimatePresence, motion, type PanInfo } from 'motion/react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { BottomSheet } from '@/components/domain/BottomSheet'
import { Drawer } from '@/components/domain/Drawer'
import { GoodsFace } from '@/components/domain/GoodsCard'
import { RadarRings } from '@/components/domain/Radar'
import { RADAR_SLOTS } from '@/components/domain/radarSlots'
import { RadarUser } from '@/components/domain/RadarUser'
import { BellIcon, ClockIcon, MenuIcon, SparkleIcon } from '@/components/ui/icons'
import { StatusBar } from '@/components/ui/StatusBar'
import { cn } from '@/lib/cn'
import { springSheet, springSnap, staggerChild, staggerParent } from '@/lib/motion'
import { ALL_WAITING, itemById, ZONES } from '@/mocks/data'
import { radarUsers, sortedWaitingList } from '@/store/matching'
import { useStore } from '@/store/useStore'

export function Home() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const [sheetOpen, setSheetOpen] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [notifOpen, setNotifOpen] = useState(false)
  const [hovered, setHovered] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
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
    setHovered(found && found !== pendingTarget ? found : null)
  }

  const onDragEnd = () => {
    setDragging(false)
    const target = hovered
    setHovered(null)
    if (!target) return
    navigate(`/poke/confirm?to=${target}`)
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

  const radarPanel = (
    <div ref={radarRef} className="relative min-h-0 flex-1 pb-[104px] md:pb-6">
      <RadarRings />

      {radar.map((user, i) => {
        const slot = RADAR_SLOTS[i]
        return (
          <div
            key={user.id}
            className="absolute"
            style={{
              top: slot.top,
              bottom: slot.bottom,
              left: slot.left,
              right: slot.right,
              transform: `translate(${slot.x}, ${slot.y})`,
            }}
          >
            <RadarUser
              user={user}
              index={i}
              hovered={hovered === user.id}
              pending={pendingTarget === user.id}
            />
          </div>
        )
      })}

      <div className="absolute top-1/2 left-1/2 z-20 -translate-x-1/2 -translate-y-1/2">
        <p className="mb-1 text-center text-[11px] font-bold text-neutral-500">내 카드</p>
        <motion.div
          drag
          dragSnapToOrigin
          dragElastic={0.9}
          dragMomentum={false}
          onDragStart={() => setDragging(true)}
          onDrag={onDrag}
          onDragEnd={onDragEnd}
          whileDrag={{ zIndex: 60, cursor: 'grabbing' }}
          className="cursor-grab touch-none"
        >
          <CardStack topItemId={topItemId} count={Math.max(haveCount, 1)} lifted={dragging} />
        </motion.div>
      </div>

      <p className="absolute inset-x-0 bottom-[84px] text-center text-[12px] text-neutral-400 md:bottom-3">
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
            <button
              type="button"
              onClick={() => navigate(`/poke/confirm?to=${user.id}`)}
              disabled={pendingTarget === user.id}
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
            </button>
          </motion.li>
        )
      })}
    </motion.ul>
  )

  return (
    <div className="flex h-full flex-col">
      <StatusBar />

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
          <motion.button
            type="button"
            aria-label="메뉴"
            onClick={() => setDrawerOpen(true)}
            whileTap={{ scale: 0.88 }}
            className="flex size-10 items-center justify-center text-ink"
          >
            <MenuIcon className="size-[22px]" />
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
              <motion.span
                className="size-1.5 rounded-full bg-ink"
                animate={{ opacity: [1, 0.25, 1] }}
                transition={{ duration: 1.2, repeat: Infinity }}
              />
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
              <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand text-ink">
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

      {/* 모바일: 레이더 위 + 바텀시트. 데스크톱: 왼쪽 레이더 + 오른쪽 목록. */}
      <div className="relative flex min-h-0 flex-1 md:hidden">
        {radarPanel}
        <BottomSheet
          open={sheetOpen}
          onOpenChange={setSheetOpen}
          peek={72}
          height={460}
          header={
            <div className="flex items-baseline justify-between">
              <span className="text-[17px] font-extrabold text-ink">전체리스트</span>
              <span className="text-[12px] text-neutral-400">{list.length}명 대기 중</span>
            </div>
          }
        >
          {listPanel}
        </BottomSheet>
      </div>

      <div className="hidden min-h-0 flex-1 gap-6 px-6 pt-3 pb-6 md:flex">
        <div className="relative flex min-h-0 flex-1 flex-col rounded-3xl bg-neutral-50/60">
          {radarPanel}
        </div>
        <aside className="flex w-[360px] shrink-0 flex-col">
          <div className="flex items-baseline justify-between px-1 pb-3">
            <h2 className="text-[17px] font-extrabold text-ink">전체리스트</h2>
            <span className="text-[12px] text-neutral-400">{list.length}명 대기 중</span>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto pr-1 no-scrollbar">{listPanel}</div>
        </aside>
      </div>

      <Drawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        have={state.have}
        needs={state.needs}
        onEditHave={() => navigate('/have')}
        onEditNeeds={() => navigate('/needs')}
        onReset={() => {
          dispatch({ type: 'reset' })
          navigate('/')
        }}
      />

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
            className="absolute inset-0 z-40 bg-black/30"
          />
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={springSheet}
            className="absolute inset-x-0 bottom-0 z-50 max-h-[62%] rounded-t-[26px] bg-white p-5 shadow-2xl"
          >
            <span aria-hidden className="mx-auto mb-4 block h-1 w-9 rounded-full bg-neutral-200" />
            <h2 className="text-[18px] font-extrabold text-ink">알림</h2>
            {notifications.length === 0 ? (
              <p className="py-10 text-center text-[13px] text-neutral-400">아직 알림이 없어요</p>
            ) : (
              <ul className="mt-3 space-y-2 overflow-y-auto no-scrollbar">
                {notifications.map((n) => (
                  <li key={n.id}>
                    <button
                      type="button"
                      onClick={() => onSelect(n.kind)}
                      className="w-full rounded-2xl bg-neutral-50 p-3.5 text-left"
                    >
                      <p className="text-[14px] font-bold text-ink">{n.title}</p>
                      <p className="text-[12px] text-neutral-400">{n.body}</p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <button
              type="button"
              onClick={onClose}
              className="mt-4 w-full rounded-full border border-neutral-200 py-3 text-[14px] font-semibold text-neutral-500"
            >
              닫기
            </button>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
