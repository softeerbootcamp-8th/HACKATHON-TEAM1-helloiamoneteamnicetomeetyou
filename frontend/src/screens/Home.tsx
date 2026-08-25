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
import { radarUsers, sortedWaitingList, wantedFromMe } from '@/store/matching'
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
  // 밀어서 치운 배너. 내용이 바뀌면 다시 뜬다.
  const [dismissedBanner, setDismissedBanner] = useState<string | null>(null)
  const bannerDragRef = useRef(false)
  const radarRef = useRef<HTMLDivElement>(null)
  // 끌었는지 기억해 둔다. 끌고 난 뒤 따라오는 click 을 걸러내는 데 쓴다.
  const draggedRef = useRef(false)

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

  const haveIds = state.have.map((h) => h.itemId)
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

  /**
   * 찔러보기로 넘어간다. 끌어다 놓아도, 상대 카드를 그냥 눌러도 여기로 온다.
   * 화면이 바로 바뀌면 무슨 일이 일어났는지 안 보여서 물결이 퍼지는 동안 붙잡아 둔다.
   */
  const sendPokeTo = (targetId: string) => {
    if (pendingTarget === targetId) return
    tick([10, 40, 14])
    setBurstOn(targetId)
    window.setTimeout(() => {
      setBurstOn(null)
      navigate(`/poke/confirm?to=${targetId}`)
    }, 700)
  }

  const onDragEnd = () => {
    setDragging(false)
    const target = hovered
    setHovered(null)
    if (!target) return
    sendPokeTo(target)
  }

  /**
   * 위에 뜨는 배너. 밀어서 치울 수 있어서 무엇을 치웠는지 구분할 id 가 필요하다.
   * 제목으로 구분하면 같은 문구의 새 알림이 와도 계속 숨어 있어서 버튼이 안 먹는 것처럼 보인다.
   */
  const banner = (() => {
    if (appointment?.stage === 'confirmed') {
      return {
        id: `appt-confirmed-${appointment.confirmedSlot}`,
        tone: 'brand' as const,
        title: `${appointment.confirmedLabel} ${zone?.name ?? ''}`,
        body: `${itemById(state.match?.giveItemId ?? topItemId).name} 거래`,
        onClick: () => navigate('/appointment'),
      }
    }
    if (appointment) {
      return {
        id: `appt-${appointment.stage}`,
        tone: 'brand' as const,
        title:
          appointment.stage === 'time-conflict' ? '시간 조율 중이에요' : '약속을 잡는 중이에요',
        body: `${itemById(state.match?.giveItemId ?? topItemId).name} 거래`,
        onClick: () => navigate('/time'),
      }
    }
    if (state.match) {
      const who =
        state.match.kind === 'ONE_TO_ONE'
          ? state.match.partner.id
          : `${state.match.giver.id}-${state.match.receiver.id}`
      return {
        id: `match-${state.match.origin}-${who}`,
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
        id: `poke-${state.incomingPoke.fromUserId}-${state.incomingPoke.wantItemId}`,
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
      className="relative flex min-h-0 flex-1 flex-col px-3 pt-4 pb-[84px] md:rounded-3xl md:bg-neutral-50/70 md:px-4 md:pt-2 md:pb-4"
    >
      {/*
        무대는 남는 공간을 그대로 쓴다. 정사각형으로 잡아 두었더니 폭에 막혀 아래쪽이
        통째로 비고 카드끼리도 붙어 보였다. 이제 세로로 긴 화면에서는 타원이 된다.
      */}
      <div className="relative mx-auto min-h-0 w-full max-w-[560px] flex-1">
        <RadarRings />

        {radar.map((user, i) => {
          const angle = (-90 + i * (360 / Math.max(radar.length, 1))) * (Math.PI / 180)
          // 가로와 세로 반지름을 따로 둬서 화면이 길수록 위아래로 더 벌어지게 한다.
          const radiusX = 37
          const radiusY = 36
          return (
            <div
              key={user.id}
              className="absolute -translate-x-1/2 -translate-y-1/2"
              style={{
                left: `${50 + radiusX * Math.cos(angle)}%`,
                top: `${50 + radiusY * Math.sin(angle)}%`,
              }}
            >
              <RadarUser
                user={user}
                index={i}
                hovered={hovered === user.id}
                pending={pendingTarget === user.id}
                burst={burstOn === user.id}
                onSelect={() => sendPokeTo(user.id)}
              />
            </div>
          )
        })}

        <div className="absolute top-1/2 left-1/2 z-20 -translate-x-1/2 -translate-y-1/2">
          <motion.button
            type="button"
            onClick={() => navigate('/have')}
            whileTap={{ scale: 0.94 }}
            transition={springSnap}
            className="mx-auto mb-1 flex items-center gap-0.5 text-[11px] font-bold text-neutral-500 md:text-[14px]"
          >
            내 카드
            <span className="text-[13px] text-neutral-300">›</span>
          </motion.button>

          {/*
            끌면 찔러보기, 그냥 누르면 내 카드 편집으로 간다. 끌고 난 뒤에도 click 이
            따라오기 때문에 끌었는지를 기억해 두고 그 click 은 버린다.
          */}
          <motion.div
            drag
            dragSnapToOrigin
            dragMomentum={false}
            dragTransition={{ bounceStiffness: 480, bounceDamping: 30 }}
            transition={springSnap}
            onDragStart={() => {
              draggedRef.current = true
              setDragging(true)
            }}
            onDrag={onDrag}
            onDragEnd={(event, info) => {
              onDragEnd()
              window.setTimeout(() => {
                draggedRef.current = false
              }, 0)
              void event
              void info
            }}
            onClick={() => {
              if (draggedRef.current) return
              navigate('/have')
            }}
            whileDrag={{ zIndex: 60, cursor: 'grabbing' }}
            data-my-cards
            className="cursor-grab touch-none"
          >
            <CardStack topItemId={topItemId} count={Math.max(haveCount, 1)} lifted={dragging} />
          </motion.div>
        </div>
      </div>

      <p className="mt-5 shrink-0 text-center text-[12px] text-neutral-400 md:text-[14px]">
        {dragging
          ? '놓아주면 찔러보기가 전송돼요'
          : '상대 카드를 누르거나, 내 카드 묶음을 끌어다 놓아보세요'}
      </p>
    </div>
  )

  /**
   * 전체리스트 한 줄. 시안대로 굿즈 이름, 내가 줄 수 있는 카드, 접속 여부를 보여준다.
   * 상대가 원하는 것 중 내가 가진 게 무엇인지가 이 줄에서 바로 보여야 누를지 말지 정한다.
   */
  const listPanel = (
    <motion.ul
      variants={staggerParent}
      initial="hidden"
      animate="show"
      className="divide-y divide-neutral-100"
    >
      {list.map((user) => {
        const item = itemById(user.itemId)
        const givable = wantedFromMe(user, haveIds)
        return (
          <motion.li key={user.id} variants={staggerChild}>
            <motion.button
              type="button"
              onClick={() => navigate(`/poke/confirm?to=${user.id}`)}
              disabled={pendingTarget === user.id}
              whileTap={pendingTarget === user.id ? undefined : { scale: 0.98 }}
              transition={springSnap}
              className={cn(
                'flex w-full items-center gap-4 py-4 text-left',
                pendingTarget === user.id && 'opacity-45',
              )}
            >
              <div className="w-[92px] shrink-0">
                <GoodsFace item={item} size="lg" />
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-[14px] font-bold text-ink">{item.name}</p>
                  {user.online ? (
                    <span className="flex shrink-0 items-center gap-1.5 text-[11px] text-[#57d1fe]">
                      <span className="size-[5px] rounded-full bg-[#57d1fe]" />
                      접속 중
                    </span>
                  ) : (
                    <span className="shrink-0 text-[11px] text-neutral-300">오프라인</span>
                  )}
                </div>

                <p className="mt-0.5 text-[10px] font-medium text-[#aeaeb2]">
                  내가 줄 수 있는 카드
                </p>
                <p className="truncate text-[11px] text-[#8b8b8b]">
                  {givable.length > 0
                    ? givable.map((id) => itemById(id).name).join(' · ')
                    : '아직 없어요'}
                </p>
              </div>

              {pendingTarget === user.id && (
                <span className="shrink-0 text-[11px] font-semibold text-neutral-400">대기 중</span>
              )}
            </motion.button>
          </motion.li>
        )
      })}
    </motion.ul>
  )

  return (
    <div className="relative flex h-full flex-col">
      <header className="flex shrink-0 items-center justify-between px-5 pt-2 md:px-10 md:pt-4">
        <h1 className="pl-1 text-[23px] font-extrabold tracking-[-0.02em] text-ink">
          교환 대기장소
        </h1>
        <div className="flex items-center gap-1">
          {/*
            목데이터만으로는 삼자 교환과 받은 요청을 보기 어려워서 넣어 둔 확인용 버튼이다.
            실제 서비스에서는 서버가 보내 주는 상황이라 그때 빼면 된다.
          */}
          <DemoButton
            label="요청받기"
            onClick={() => dispatch({ type: 'seed-demo', kind: 'incoming' })}
          />
          <DemoButton
            label="3인 매칭"
            onClick={() => dispatch({ type: 'seed-demo', kind: 'three-way' })}
          />

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

      {/*
        알림이 와도 아래가 밀리지 않게 한다.
        모바일은 이 칸의 높이를 고정해서 "자동 매칭 중" 알약이든 배너든 같은 자리를 쓰고,
        데스크톱은 아예 흐름에서 빼서 화면 위에 띄운다. 둘 다 레이더가 움직이지 않는다.
      */}
      <div className="min-h-[78px] shrink-0 px-5 pt-2 md:pointer-events-none md:absolute md:top-[76px] md:left-10 md:z-30 md:min-h-0 md:w-[360px] md:px-0 md:[&>*]:pointer-events-auto">
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

          {banner && banner.id !== dismissedBanner && (
            <motion.button
              key={banner.id}
              type="button"
              drag
              dragConstraints={{ left: 0, right: 0, top: 0, bottom: 0 }}
              dragElastic={{ left: 0.5, right: 0.5, top: 0.5, bottom: 0.05 }}
              dragMomentum={false}
              dragSnapToOrigin
              onDragStart={() => {
                bannerDragRef.current = true
              }}
              onDragEnd={(_, info) => {
                // 옆으로 밀거나 위로 올리면 치운다. 아래로는 안 치운다.
                const flung =
                  Math.abs(info.offset.x) > 90 ||
                  Math.abs(info.velocity.x) > 500 ||
                  info.offset.y < -60 ||
                  info.velocity.y < -500
                if (flung) {
                  tick(10)
                  setDismissedBanner(banner.id)
                }
                window.setTimeout(() => {
                  bannerDragRef.current = false
                }, 0)
              }}
              onClick={() => {
                if (bannerDragRef.current) return
                banner.onClick()
              }}
              initial={{ opacity: 0, y: -12, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -10, scale: 0.98 }}
              transition={springSheet}
              whileTap={{ scale: 0.98 }}
              className={cn(
                'flex w-full cursor-grab touch-pan-y items-center gap-3 rounded-2xl p-3.5 text-left active:cursor-grabbing',
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
      <div className="relative flex min-h-0 flex-1 flex-col overflow-hidden md:flex-row md:gap-10 md:px-10 md:pb-8">
        {radarPanel}

        <aside className="hidden w-[360px] shrink-0 flex-col md:flex lg:w-[420px]">
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
        onDismiss={(id) => dispatch({ type: 'read-notification', id })}
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

/** 확인용 버튼. 목데이터로는 잘 안 나오는 상황을 손으로 만들어 볼 수 있게 한다. */
function DemoButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <motion.button
      type="button"
      onClick={onClick}
      whileTap={{ scale: 0.92 }}
      transition={springSnap}
      className="rounded-full border border-dashed border-neutral-300 px-2.5 py-1 text-[11px] font-semibold text-neutral-400"
    >
      {label}
    </motion.button>
  )
}

/**
 * 알림 한 줄. 왼쪽으로 밀면 지워진다. 쌓이기만 하고 못 지우면 금방 지저분해진다.
 */
function NotificationRow({
  notification,
  onSelect,
  onDismiss,
}: {
  notification: { id: string; kind: string; title: string; body: string }
  onSelect: (kind: string) => void
  onDismiss: (id: string) => void
}) {
  // 밀어서 지운 뒤에도 click 이 뒤따라 와서 알림이 열려 버린다. 끌었으면 그 click 은 버린다.
  const draggedRef = useRef(false)

  return (
    <motion.li
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: -80, height: 0, marginTop: 0 }}
      transition={springSnap}
      className="relative overflow-hidden rounded-2xl"
    >
      <span className="absolute inset-y-0 right-0 flex w-24 items-center justify-center rounded-2xl bg-rose-50 text-[12px] font-bold text-rose-500">
        지우기
      </span>

      <motion.button
        type="button"
        drag="x"
        dragConstraints={{ left: -110, right: 0 }}
        dragElastic={{ left: 0.15, right: 0 }}
        dragMomentum={false}
        dragSnapToOrigin
        onDragStart={() => {
          draggedRef.current = true
        }}
        onDragEnd={(_, info) => {
          if (info.offset.x < -70 || info.velocity.x < -450) {
            tick(12)
            onDismiss(notification.id)
          }
          // click 이 이 뒤에 한 번 더 온다. 다음 tick 까지만 막아 두면 된다.
          window.setTimeout(() => {
            draggedRef.current = false
          }, 0)
        }}
        onClick={() => {
          if (draggedRef.current) return
          onSelect(notification.kind)
        }}
        whileTap={{ scale: 0.98 }}
        transition={springSnap}
        className="relative w-full cursor-grab touch-pan-y rounded-2xl bg-neutral-50 p-3.5 text-left active:cursor-grabbing"
      >
        <p className="text-[14px] font-bold text-ink">{notification.title}</p>
        <p className="text-[12px] text-neutral-400">{notification.body}</p>
      </motion.button>
    </motion.li>
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
  onDismiss,
}: {
  open: boolean
  onClose: () => void
  notifications: { id: string; kind: string; title: string; body: string }[]
  onSelect: (kind: string) => void
  onDismiss: (id: string) => void
}) {
  const body =
    notifications.length === 0 ? (
      <p className="py-10 text-center text-[13px] text-neutral-400">아직 알림이 없어요</p>
    ) : (
      <ul className="mt-3 space-y-2">
        <AnimatePresence initial={false}>
          {notifications.map((n) => (
            <NotificationRow
              key={n.id}
              notification={n}
              onSelect={onSelect}
              onDismiss={onDismiss}
            />
          ))}
        </AnimatePresence>
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
            data-sheet="notifications"
            className="absolute inset-x-0 bottom-0 z-50 flex max-h-[62%] flex-col rounded-t-[26px] bg-white p-5 pb-[max(1.25rem,env(safe-area-inset-bottom))] shadow-2xl md:hidden"
          >
            <span
              aria-hidden
              className="mx-auto mb-4 block h-1 w-9 shrink-0 rounded-full bg-neutral-200"
            />
            <h2 className="shrink-0 text-[18px] font-extrabold text-ink">알림</h2>
            {/* 목록만 굴러가야 한다. 높이를 안 묶으면 시트가 통째로 늘어난다. */}
            <div className="min-h-0 flex-1 overflow-y-auto no-scrollbar">{body}</div>
            <motion.button
              type="button"
              onClick={onClose}
              whileTap={{ scale: 0.97 }}
              transition={springSnap}
              className="mt-4 w-full shrink-0 rounded-full border border-neutral-200 py-3 text-[14px] font-semibold text-neutral-500"
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
            <div className="flex shrink-0 items-center justify-between">
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
