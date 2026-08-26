import { AnimatePresence, motion, type PanInfo } from 'motion/react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router'

import { AppointmentStatusRail } from '@/components/domain/AppointmentStatus'
import { BottomSheet } from '@/components/domain/BottomSheet'
import { CardStack } from '@/components/domain/CardStack'
import { GoodsFace, ItemCard } from '@/components/domain/GoodsCard'
import { PushOptInBanner } from '@/components/domain/PushOptInBanner'
import { RadarRings } from '@/components/domain/Radar'
import { RadarUser } from '@/components/domain/RadarUser'
import { BellIcon } from '@/components/ui/icons'
import { useCatalog } from '@/features/catalog/useCatalog'
import { useNotification } from '@/features/notification/useNotification'
import type { BoothHaveItem } from '@/features/poke/api'
import { usePoke } from '@/features/poke/usePoke'
import { cn } from '@/lib/cn'
import { tick } from '@/lib/haptics'
import { springSheet, springSnap, staggerChild, staggerParent } from '@/lib/motion'
import { usePush, type PushState } from '@/lib/use-push'
import type { Item } from '@/features/catalog/api'
import { useItem } from '@/features/catalog/useItem'
import { appointmentStatus, sortedAppointments } from '@/store/appointment-status'
import { getDeviceId } from '@/store/identity'
import type { WaitingStatus } from '@/store/matching'
import { persistSetupDone } from '@/store/setup-status'
import { byPresence, topItemIdOf } from '@/store/top-card'
import type { Selection } from '@/store/types'
import { useStore } from '@/store/useStore'

export function Home() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const {
    waiting: serverWaiting,
    sent,
    error: pokeError,
    clearError,
    refresh: refreshPokes,
  } = usePoke()
  const { state: catalog } = useCatalog()
  const {
    notifications: serverNotifications,
    unreadCount,
    markRead: markNotificationRead,
  } = useNotification()
  const [sheetOpen, setSheetOpen] = useState(false)
  const [notifOpen, setNotifOpen] = useState(false)
  // 앱을 닫아 둔 사이의 알림. 여는 자리는 알림 목록 위다.
  const { state: pushState, enable: enablePush } = usePush(getDeviceId())
  const [hovered, setHovered] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  // 방금 카드를 놓은 상대. 고리가 한 번 터지고 나서 찔러보기 확인 화면으로 넘어간다.
  const [burstOn, setBurstOn] = useState<string | null>(null)
  /**
   * 서버 레이더에서 지금까지 넘긴 카드 수.
   *
   * <b>누른 횟수로 곱셈을 하면 안 된다.</b> 한 번에 채우는 칸 수는 답변 대기 중인 카드가
   * 몇 장이냐에 따라 5에서 4, 3으로 줄어든다. 그러면 보폭이 바뀌어서 다음 장이 앞으로
   * 당겨지고, 방금 본 카드가 도로 올라온다. 넘긴 개수를 그대로 들고 있어야 한다.
   */
  const [radarCursor, setRadarCursor] = useState(0)
  // 내 카드 묶음을 눌러서 펼쳐 본 상태. 펼친 동안에는 끌어놓기를 하지 않는다.
  const [fanOpen, setFanOpen] = useState(false)
  const bannerDragRef = useRef(false)
  const radarRef = useRef<HTMLDivElement>(null)
  // 끌었는지 기억해 둔다. 끌고 난 뒤 따라오는 click 을 걸러내는 데 쓴다.
  const draggedRef = useRef(false)

  useEffect(() => {
    dispatch({ type: 'enter-home' })
    // 이 기기가 홈까지 와 봤다는 것을 기억해 둔다. 다음에 들어오면 온보딩을 건너뛴다.
    persistSetupDone()
    // 홈에 들어올 때 한 번만 자동 매칭을 켠다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  /**
   * 홈에 들어올 때마다 목록을 다시 읽는다.
   *
   * <b>각 줄의 `wanted` 와 "내가 줄 수 있는 카드" 는 서버가 내 등록 내용과 견줘 계산한다.</b>
   * 그래서 찾는 카드를 고치고 돌아오면 그 값이 이미 낡아 있다. 다시 읽지 않으면 방금 등록한
   * 카드가 레이더에 뜨지 않아서 새로고침해야 보이는 것처럼 된다.
   */
  useEffect(() => {
    refreshPokes()
  }, [refreshPokes])

  // 찔러보기 요청이 실패한 사유를 띄운다. 서버 message 는 그대로 보여줘도 되는 한글
  // 문장이라는 것이 팀 약속이다. 띄운 뒤에는 지워서 화면을 옮겨도 다시 뜨지 않게 한다.
  useEffect(() => {
    if (!pokeError) return
    dispatch({ type: 'toast', message: pokeError })
    clearError()
  }, [pokeError, dispatch, clearError])

  // 배열을 그대로 의존성에 넣으면 렌더마다 새 참조라 매번 다시 계산한다.
  // 문자열 하나로 눌러서 값이 실제로 바뀔 때만 돌게 한다.
  const needKey = state.needs.map((s) => s.itemId).join(',')
  const needIds = useMemo(() => (needKey ? needKey.split(',').map(Number) : []), [needKey])

  const itemById = catalog.status === 'ready' ? catalog.itemById : undefined

  // 답을 기다리는 상대. 그 사람 카드는 다시 보낼 수 없게 잠근다 (시안 desc 165:3514).
  const pendingOwnerIds = useMemo(
    () => new Set(sent.filter((p) => p.status === 'PENDING').map((p) => p.targetUserId)),
    [sent],
  )

  /**
   * 레이더에 세울 상대를 서버 목록에서 뽑는다.
   *
   * 시안 규칙이다 (desc 165:3500 2번) — 먼저 등록된 순으로 최대 5개, 카드 종류마다 한 명씩.
   * 무엇을 담을지는 서버가 이미 정해서 준다(희망 카드가 있으면 그와 맞는 것만, 없으면 내가
   * 가진 카드를 뺀 전부). 여기서 다시 걸러내지 않는다.
   *
   * 목록이 카드를 통째로 싣고 오기 때문에 여기서 카드를 따로 찾지 않는다.
   */
  const serverRadarPool = useMemo(() => {
    const seen = new Set<number>()
    return serverWaiting
      .slice()
      .sort((a, b) => a.haveItemId - b.haveItemId)
      .flatMap((row) => {
        if (seen.has(row.item.id)) return []
        seen.add(row.item.id)
        return [row]
      })
  }, [serverWaiting])

  /** 답변을 기다리는 카드가 자리를 지키고 남은 칸. 커서를 미는 보폭이기도 하다. */
  const radarRoom = useMemo(() => {
    const held = serverRadarPool.filter((row) => pendingOwnerIds.has(row.ownerId)).length
    return Math.max(5 - Math.min(held, 5), 0)
  }, [serverRadarPool, pendingOwnerIds])

  /**
   * 지금 세울 다섯 명. "다른 카드 보기" 를 누르면 뒷순위로 넘어간다.
   *
   * 답변을 기다리는 상대는 자리를 지킨다. 시안이 "찔러보기 답변 대기 중 상태인 아이템은
   * 새로고침하지 않고 화면에 계속 표시" 라고 못박아 뒀다(desc 165:3500 4번).
   */
  const serverRadar = useMemo(() => {
    const held = serverRadarPool.filter((row) => pendingOwnerIds.has(row.ownerId)).slice(0, 5)
    const rest = serverRadarPool.filter((row) => !pendingOwnerIds.has(row.ownerId))
    const rotated =
      rest.length === 0
        ? []
        : Array.from({ length: Math.min(radarRoom, rest.length) }, (_, i) => {
            return rest[(radarCursor + i) % rest.length]
          })
    return [...held, ...rotated]
  }, [serverRadarPool, pendingOwnerIds, radarRoom, radarCursor])

  /** 레이더 한 칸. 배치와 끌어놓기가 이 모양 하나만 본다. */
  const radarSlots: {
    targetId: string
    item: Item
    label: string
    pending: boolean
  }[] = serverRadar.map((row) => ({
    // 표적은 보유 등록 줄이다. 같은 사람이 여러 카드를 내놓아도 어느 카드를 달라는
    // 것인지가 이 값으로 정해진다.
    targetId: String(row.haveItemId),
    item: row.item,
    label: row.item.name,
    pending: pendingOwnerIds.has(row.ownerId),
  }))

  const haveCount = state.have.reduce((sum, s) => sum + s.qty, 0)

  const topItemId = topItemIdOf(state.have)

  // 약속이 둘 이상이면 가까운 순으로 늘어놓고 가로로 밀어서 본다.
  const statuses = sortedAppointments(state.appointments).map((appointment) =>
    appointmentStatus(appointment, (id) => itemById?.(id)),
  )

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

  /**
   * 이 표적에 지금 보낼 수 있는지.
   *
   * 답을 기다리는 상대는 막는다. 끌어놓는 동작 자체는 되지만 강조 표시가 붙지 않아서 보낼
   * 수 없다는 것이 손끝으로 전해진다 (시안 desc 165:3514 "드래그 앤 드롭은 가능하나,
   * 이미지 상태 변화 X & 해당 화면 잔류").
   */
  const isPendingTarget = (targetId: string): boolean =>
    radarSlots.some((slot) => slot.targetId === targetId && slot.pending)

  const onDrag = (_: unknown, info: PanInfo) => {
    const found = hitTest(info.point)
    const next = found && !isPendingTarget(found) ? found : null
    // 대상에 처음 올라탄 순간에만 울린다. 매 프레임 울리면 손이 얼얼해진다.
    if (next && next !== hovered) tick(8)
    setHovered(next)
  }

  /**
   * 찔러보기로 넘어간다. 끌어다 놓아도, 상대 카드를 그냥 눌러도 여기로 온다.
   * 화면이 바로 바뀌면 무슨 일이 일어났는지 안 보여서 물결이 퍼지는 동안 붙잡아 둔다.
   */
  const sendPokeTo = (targetId: string) => {
    if (isPendingTarget(targetId)) return
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
   * 알림을 눌렀을 때 하는 일. 읽음으로 표시하고 해당 화면을 연다.
   *
   * 문구와 열리는 화면의 짝은 백엔드 `PushMessage` 와 같은 것을 쓴다. 배너와 알림 패널이
   * 같은 표를 봐야 해서 한 곳에 둔다. `PushMessage.url` 을 고치면 여기도 같이 고친다.
   */
  const openNotification = (id: number, kind: string) => {
    void markNotificationRead(id)

    if (kind === 'POKE_RECEIVED') navigate('/poke/received')
    else if (kind === 'MATCH_SUGGESTED' || kind === 'MATCH_ACCEPTED' || kind === 'POKE_ACCEPTED')
      navigate('/match')
    else if (
      kind === 'EXCHANGE_TIME_REQUESTED' ||
      kind === 'EXCHANGE_TIME_MATCHED' ||
      kind === 'EXCHANGE_TIME_MISMATCHED' ||
      kind === 'EXCHANGE_TIME_AGREED'
    )
      navigate('/time')
    else if (
      kind === 'EXCHANGE_CREATED' ||
      kind === 'EXCHANGE_TIME_UPDATED' ||
      kind === 'EXCHANGE_PLACE_UPDATED'
    )
      navigate('/appointment')
    else if (kind === 'MATCH_REJECTED' || kind === 'POKE_REJECTED' || kind === 'EXCHANGE_CANCELLED')
      navigate('/home')
  }

  /**
   * 위에 뜨는 알림 카드. 시안의 `알림 수신 예시`(204:5026) 자리다.
   *
   * <b>서버 알림 목록의 맨 앞 한 건을 그대로 쓴다.</b> 예전에는 화면이 들고 있는 상태
   * (`state.match`, 받은 찔러보기)로 직접 만들었는데, 그러면 시간 조율이나 약속 취소처럼
   * 화면 상태에 없는 알림은 배너로 뜰 길이 없었고 문구도 백엔드와 두 벌이 됐다.
   * 목록에는 안 읽은 것만 들어 있고 서버가 최근 순으로 준다.
   */
  const banner = serverNotifications[0] ?? null

  /**
   * 레이더는 정사각형 무대 안에 그린다. 예전에는 세로로 늘어나는 칸에 백분율로 카드를
   * 붙였는데, 칸 높이가 화면마다 달라서 카드가 원에서 벗어나 서로 겹쳤다.
   * 이제 원 둘레를 각도로 나눠 앉히므로 어느 화면에서도 배치가 같다.
   */
  const radarPanel = (
    <div
      ref={radarRef}
      className="relative flex min-h-0 flex-1 flex-col px-1 pt-0 pb-[84px] md:rounded-3xl md:bg-neutral-50/70 md:px-4 md:pt-2 md:pb-4"
    >
      {/*
        무대는 정사각형이다. 시안의 링이 정원이라 무대가 찌그러지면 원도 같이 찌그러진다.
        폭을 기준으로 잡되 세로로 짧은 화면에서 넘치지 않게 화면 높이로도 묶어 둔다.
      */}
      <div className="relative mx-auto flex min-h-0 w-full max-w-[560px] -translate-y-3 flex-1 flex-col items-center justify-center">
        <div className="relative aspect-square w-[min(92%,44vh)] shrink-0">
          <RadarRings />

          {radarSlots.map((slot, i) => {
            const angle = (-90 + i * (360 / Math.max(radarSlots.length, 1))) * (Math.PI / 180)
            // 가로와 세로 반지름을 따로 둬서 화면이 길수록 위아래로 더 벌어지게 한다.
            const radiusX = 41.5
            const radiusY = 41.5
            return (
              <div
                key={slot.targetId}
                className="absolute -translate-x-1/2 -translate-y-1/2"
                style={{
                  left: `${50 + radiusX * Math.cos(angle)}%`,
                  top: `${50 + radiusY * Math.sin(angle)}%`,
                }}
              >
                <RadarUser
                  targetId={slot.targetId}
                  item={slot.item}
                  label={slot.label}
                  index={i}
                  hovered={hovered === slot.targetId}
                  pending={slot.pending}
                  burst={burstOn === slot.targetId}
                  onSelect={() => sendPokeTo(slot.targetId)}
                />
              </div>
            )
          })}

          {/*
            펼치면 카드 묶음이 화면을 덮는 층(`MyCardsOverlay`)으로 옮겨 간다. 여기 남는
            것은 접혀 있을 때의 끌 수 있는 묶음뿐이라, 펼친 동안에는 이 자리를 비운다.
          */}
          {!fanOpen && (
            <div className="absolute top-1/2 left-1/2 z-20 -translate-x-1/2 -translate-y-1/2">
              <p className="mb-1 text-center text-[11px] font-bold text-ink md:text-[14px]">
                내 카드
              </p>

              {/*
                끌면 찔러보기, 그냥 누르면 내 카드를 펼쳐 본다. 끌고 난 뒤에도 click 이
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
                  setFanOpen(true)
                }}
                whileDrag={{ zIndex: 60, cursor: 'grabbing' }}
                data-my-cards
                className="cursor-grab touch-none"
              >
                <CardStack topItemId={topItemId} count={Math.max(haveCount, 1)} lifted={dragging} />
              </motion.div>
            </div>
          )}
        </div>

        <p className="mt-8 shrink-0 text-center text-[12px] text-neutral-400 md:text-[14px]">
          {dragging ? '놓아주면 찔러보기가 전송돼요' : '내 카드를 상대 카드 위로 끌어보세요'}
        </p>

        {/* 레이더에 올라온 카드를 뒷순위로 새로 채운다. 답변을 기다리는 카드는 남는다. */}
        <div className="mt-2 shrink-0 text-center">
          <motion.button
            type="button"
            onClick={() => {
              tick(8)
              setRadarCursor((cursor) => cursor + radarRoom)
            }}
            whileTap={{ scale: 0.94 }}
            transition={springSnap}
            className="rounded-full bg-neutral-200 px-4 py-1.5 text-[11px] font-bold text-neutral-500"
          >
            다른 카드 보기
          </motion.button>
        </div>
      </div>
    </div>
  )

  /**
   * 전체리스트 한 줄. 시안대로 굿즈 이름, 내가 줄 수 있는 카드, 매칭 상태를 보여준다.
   * 상대가 원하는 것 중 내가 가진 게 무엇인지가 이 줄에서 바로 보여야 누를지 말지 정한다.
   */
  /**
   * 서버에 등록한 사람들의 카드. 배지는 시안 desc 204:4948 기준으로 가른다.
   *
   * "매칭됨" 은 서버가 `matched` 로 내려준다. 화면이 들고 있는 매칭 상태로 판단하면
   * 알림을 놓치거나 새로고침한 순간 이미 매칭된 상대가 "교환 가능" 으로 되돌아간다.
   */
  const serverListPanel =
    serverWaiting.length === 0 ? (
      <p className="py-10 text-center text-[13px] leading-[1.7] text-neutral-400">
        {needIds.length === 0
          ? '아직 이 부스에 카드를 내놓은 사람이 없어요.'
          : '아직 이 부스에 찾는 카드를 내놓은 사람이 없어요.'}
      </p>
    ) : (
      <motion.ul
        variants={staggerParent}
        initial="hidden"
        animate="show"
        className="divide-y divide-neutral-100"
      >
        {serverWaiting.map((row) => {
          const waitingReply = pendingOwnerIds.has(row.ownerId)
          const status = waitingStatusOf(row)

          return (
            <motion.li key={row.haveItemId} variants={staggerChild}>
              <motion.button
                type="button"
                onClick={() => navigate(`/poke/confirm?to=${row.haveItemId}`)}
                disabled={waitingReply}
                whileTap={waitingReply ? undefined : { scale: 0.98 }}
                transition={springSnap}
                className={cn(
                  'flex w-full items-center gap-4 py-4 text-left',
                  waitingReply && 'opacity-45',
                )}
              >
                <div className="w-[92px] shrink-0">
                  <GoodsFace item={row.item} size="md" />
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <p className="truncate text-[14px] font-bold text-ink">{row.item.name}</p>
                    <WaitingStatusTag status={status} />
                  </div>

                  <p className="mt-0.5 text-[10px] font-medium text-[#aeaeb2]">
                    내가 건넬 수 있는 카드
                  </p>
                  <p className="truncate text-[11px] text-[#8b8b8b]">
                    {row.givableItemNames.length > 0
                      ? row.givableItemNames.join(' · ')
                      : '아직 없어요'}
                  </p>
                </div>

                {waitingReply && (
                  <span className="shrink-0 text-[11px] font-semibold text-neutral-400">
                    대기 중
                  </span>
                )}
              </motion.button>
            </motion.li>
          )
        })}
      </motion.ul>
    )

  const listPanel = serverListPanel

  /**
   * 머리에 적히는 개수. <b>아이템 기준으로 센다</b>(시안 desc 165:3500 5번).
   * 같은 카드를 세 사람이 내놓았어도 고를 수 있는 카드는 한 종류다.
   */
  const listCount = new Set(serverWaiting.map((row) => row.item.id)).size

  const listHeader = (
    <div className="flex items-end justify-between">
      {/* 시안 desc 165:3500 5번 — 접힌 머리에는 제목과 전체 개수만 있다. */}
      <span className="text-[17px] font-extrabold text-ink">지금 대기존에 올라온 카드</span>
      <span className="text-[12px] text-neutral-400">{listCount}개 대기 중</span>
    </div>
  )

  return (
    <div className="relative flex h-full flex-col">
      <header className="flex shrink-0 items-center justify-between px-5 pt-2 md:px-10 md:pt-4">
        <h1 className="pl-1 text-[23px] font-extrabold tracking-[-0.02em] text-ink">
          교환 대기존 🎡
        </h1>
        <div className="flex items-center gap-1">
          <motion.button
            type="button"
            aria-label="알림"
            onClick={() => setNotifOpen(true)}
            whileTap={{ scale: 0.88 }}
            className="relative flex size-10 items-center justify-center text-ink"
          >
            <BellIcon className="size-[22px]" />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 size-2 rounded-full bg-brand" />
            )}
          </motion.button>
        </div>
      </header>

      {/*
        알림이 와도 아래가 밀리지 않게 한다.
        모바일은 이 칸의 높이를 고정해서 "자동 매칭 중" 알약이든 약속 상태 카드든 같은 자리를
        쓰고, 데스크톱은 아예 흐름에서 빼서 화면 위에 띄운다. 둘 다 레이더가 움직이지 않는다.
      */}
      <div className="min-h-[60px] shrink-0 px-5 pt-1 md:pointer-events-none md:absolute md:top-[76px] md:left-10 md:z-30 md:min-h-0 md:w-[360px] md:px-0 md:[&>*]:pointer-events-auto">
        <AnimatePresence mode="popLayout">
          {statuses.length > 0 ? (
            <motion.div
              key="appointments"
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              transition={springSnap}
            >
              <AppointmentStatusRail
                statuses={statuses}
                onSelect={(status) => {
                  dispatch({ type: 'select-appointment', id: status.id })
                  navigate(status.to)
                }}
              />
            </motion.div>
          ) : (
            state.autoMatching && (
              <motion.span
                key="matching"
                initial={{ opacity: 0, y: -6, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95 }}
                transition={springSnap}
                className="inline-flex items-center gap-1.5 rounded-full bg-brand px-3.5 py-1.5 text-[12px] font-bold text-white"
              >
                <span className="anim-blink size-1.5 rounded-full bg-alarm" />
                교환 상대 찾는 중...
              </motion.span>
            )
          )}
        </AnimatePresence>

        <AnimatePresence>
          {banner && (
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
                  // 서버에 읽음으로 남긴다. 화면에서만 치우면 새로고침에 다시 올라온다.
                  void markNotificationRead(banner.id)
                }
                window.setTimeout(() => {
                  bannerDragRef.current = false
                }, 0)
              }}
              onClick={() => {
                if (bannerDragRef.current) return
                openNotification(banner.id, banner.type)
              }}
              initial={{ opacity: 0, y: -12, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -10, scale: 0.98 }}
              transition={springSheet}
              whileTap={{ scale: 0.98 }}
              className="mt-2 flex w-full cursor-grab touch-pan-y items-center gap-3 rounded-2xl bg-white p-3.5 text-left shadow-[0_4px_18px_rgba(0,0,0,0.10)] active:cursor-grabbing"
            >
              <img
                src="/logo.svg"
                alt=""
                aria-hidden
                className={cn(
                  'size-9 shrink-0 rounded-xl',
                  banner.type === 'POKE_ACCEPTED' && 'anim-pop',
                )}
              />
              <span className="min-w-0 flex-1">
                <span className="block truncate text-[14px] font-bold text-ink">
                  {banner.title}
                </span>
                <span className="block text-[12px] text-neutral-500">탭하여 확인</span>
              </span>
              <span className="text-[18px] text-neutral-300">›</span>
            </motion.button>
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
          <div className="px-1 pb-3">{listHeader}</div>
          <div className="min-h-0 flex-1 overflow-y-auto pr-1 pb-2 no-scrollbar">{listPanel}</div>
        </aside>

        <div className="md:hidden">
          <BottomSheet
            open={sheetOpen}
            onOpenChange={setSheetOpen}
            peek={86}
            height={460}
            header={listHeader}
          >
            {listPanel}
          </BottomSheet>
        </div>
      </div>

      {/*
        내 카드를 펼친 층. <b>화면 전체를 덮는다.</b> 예전에는 레이더 무대(정사각형) 안에만
        깔려서 헤더도 바텀시트도 덮지 못했고, 카드 뒤에 회색 네모 한 장이 떠 있는 것처럼
        보였다. 여기가 화면 껍데기 바로 밑이라 `inset-0` 이 곧 화면 전체다.
      */}
      <AnimatePresence>
        {fanOpen && (
          <MyCardsOverlay
            have={state.have}
            count={haveCount}
            onEdit={() => {
              setFanOpen(false)
              navigate('/have')
            }}
            onClose={() => setFanOpen(false)}
          />
        )}
      </AnimatePresence>

      <NotificationSheet
        open={notifOpen}
        onClose={() => setNotifOpen(false)}
        notifications={serverNotifications.map((n) => ({
          id: String(n.id),
          kind: n.type,
          title: n.title,
        }))}
        pushState={pushState}
        onEnablePush={enablePush}
        onDismiss={(id) => void markNotificationRead(Number(id))}
        onSelect={(id, kind) => {
          setNotifOpen(false)
          openNotification(Number(id), kind)
        }}
      />
    </div>
  )
}

/**
 * 서버 목록 한 줄의 상태. 세 가지를 위에서부터 본다 (시안 desc 204:4948).
 *
 * 매칭된 상대에게 줄 카드가 있어도 "매칭됨" 이 먼저다. 이미 만나기로 한 사람을
 * "교환 가능" 으로 두면 아직 아무것도 정해지지 않은 것처럼 읽힌다.
 */
function waitingStatusOf(row: BoothHaveItem): WaitingStatus {
  if (row.matched) return '매칭됨'
  return row.givableItemNames.length > 0 ? '교환 가능' : '그래도 찔러보기'
}

/** 전체리스트 오른쪽 상태. 매칭됐거나 교환이 되는 상대만 브랜드색으로 눈에 띈다. */
function WaitingStatusTag({ status }: { status: WaitingStatus }) {
  const dim = status === '그래도 찔러보기'
  return (
    <span
      className={cn(
        'flex shrink-0 items-center gap-1.5 text-[11px] font-semibold',
        dim ? 'text-neutral-300' : 'text-brand',
      )}
    >
      <span className={cn('size-[5px] rounded-full', dim ? 'bg-neutral-300' : 'bg-brand')} />
      {status}
    </span>
  )
}

/**
 * 격자로 편 카드 한 장. 카드를 찾는 훅을 쓰려면 컴포넌트로 나와 있어야 한다.
 * 부스 목록에 없는 카드면 그 자리를 비운다.
 */
function FanGridCard({ selection, index }: { selection: Selection; index: number }) {
  const item = useItem(selection.itemId)
  if (!item) return null

  return (
    <motion.div
      initial={{ opacity: 0, y: 10, scale: 0.94 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ ...springSnap, delay: index * 0.03 }}
    >
      <ItemCard item={item} size="sm" className="shadow-[0_4px_16px_rgba(0,0,0,0.10)]">
        <QtyBadge qty={selection.qty} />
      </ItemCard>
    </motion.div>
  )
}

/**
 * 내 카드를 펼쳤을 때 화면을 덮는 층.
 *
 * <b>덮는 범위가 화면 전체다.</b> 펼친 동안에는 내 카드만 보여야 하는데, 이 층이 레이더
 * 무대 안에 있으면 헤더와 바텀시트가 그대로 남고 무대 크기의 회색 네모만 한 장 뜬다.
 * 그래서 화면 껍데기 바로 밑에 두고 `inset-0` 으로 깐다. 바텀시트가 `z-30` 이라 그보다 위다.
 *
 * 카드와 글자는 클릭을 받지 않는다. 카드든 카드 사이 빈 곳이든 누르면 그대로 뒤에 깔린
 * 접기 판으로 떨어져서 접힌다. 되살리는 것은 `FanActions` 의 버튼 둘뿐이다.
 */
function MyCardsOverlay({
  have,
  count,
  onEdit,
  onClose,
}: {
  have: Selection[]
  /** 들고 있는 총 장수. 종류 수는 `have.length` 다. */
  count: number
  onEdit: () => void
  onClose: () => void
}) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={springSnap}
      className="absolute inset-0 z-40 flex items-center justify-center bg-neutral-100/90 backdrop-blur-[3px]"
    >
      <button
        type="button"
        aria-label="내 카드 접기"
        onClick={onClose}
        className="absolute inset-0"
      />

      <div className="pointer-events-none relative flex flex-col items-center">
        <p className="mb-1 text-center text-[11px] font-bold text-ink md:text-[14px]">
          내 카드 {have.length}종 {count}장
        </p>

        <MyCardsFan have={have} onEdit={onEdit} onClose={onClose} />

        <p className="mt-6 text-center text-[12px] text-neutral-400 md:text-[14px]">
          아무 곳이나 누르면 닫혀요
        </p>
      </div>
    </motion.div>
  )
}

/** 부채꼴로 세울 종류 수. 이보다 많으면 격자로 편다. */
const FANNED_KINDS = 3
/** 격자에 늘어놓을 종류 수. 한 줄에 세 장씩 세 줄까지만 보여준다. */
const GRID_KINDS = 9

/**
 * 눌러서 펼친 내 카드 묶음. 시안의 `4_v1`, `4_v2` 자리다.
 *
 * 많이 들고 있는 카드부터 보여준다. 세 종류까지는 시안대로 부채꼴로 세우고, 그보다
 * 많으면 부채꼴에 끼워 넣어 봐야 이름이 서로 가리기 때문에 한 줄에 세 장씩 격자로 편다.
 * 무엇을 몇 장 들고 있는지가 이 화면에서 다 보여야 한다.
 */
function MyCardsFan({
  have,
  onEdit,
  onClose,
}: {
  have: Selection[]
  onEdit: () => void
  onClose: () => void
}) {
  const sorted = [...have].sort(byPresence)

  if (sorted.length === 0) {
    return (
      <div className="w-[300px]">
        <p className="py-10 text-center text-[12px] text-neutral-400">아직 고른 카드가 없어요</p>
        <FanActions onEdit={onEdit} onClose={onClose} />
      </div>
    )
  }

  if (sorted.length <= FANNED_KINDS) {
    return (
      <div className="w-[350px]">
        <div className="flex items-end justify-center">
          {sorted.map((selection, i) => {
            // 가운데 카드를 가장 높이 세우고 양옆으로 눕힌다.
            const fromCenter = i - (sorted.length - 1) / 2
            return (
              <motion.div
                key={selection.itemId}
                initial={{ opacity: 0, y: 14, rotate: 0 }}
                animate={{
                  opacity: 1,
                  y: Math.abs(fromCenter) * 16,
                  rotate: fromCenter * 10,
                }}
                transition={{ ...springSnap, delay: i * 0.04 }}
                style={{ zIndex: 10 - Math.round(Math.abs(fromCenter) * 2) }}
                className="relative -mx-1.5"
              >
                <CardStack topItemId={selection.itemId} count={selection.qty} />
                <QtyBadge qty={selection.qty} />
              </motion.div>
            )
          })}
        </div>

        <FanActions onEdit={onEdit} onClose={onClose} />
      </div>
    )
  }

  const shown = sorted.slice(0, GRID_KINDS)
  const hiddenKinds = sorted.length - shown.length

  return (
    <div className="w-[300px]">
      <div className="grid grid-cols-3 gap-2">
        {shown.map((selection, i) => (
          <FanGridCard key={selection.itemId} selection={selection} index={i} />
        ))}
      </div>

      {hiddenKinds > 0 && (
        <p className="mt-2 text-center text-[11px] font-bold text-neutral-500">
          외 {hiddenKinds}종 더
        </p>
      )}

      <FanActions onEdit={onEdit} onClose={onClose} />
    </div>
  )
}

/** 카드에 붙는 장수 표시. 한 장이면 굳이 적지 않는다. */
function QtyBadge({ qty }: { qty: number }) {
  if (qty <= 1) return null
  return (
    <span className="absolute -top-1.5 -right-1 rounded-full bg-ink px-1.5 py-0.5 text-[10px] font-bold text-white">
      {qty}장
    </span>
  )
}

/**
 * 펼친 카드 밑에 서는 버튼 둘.
 *
 * 닫기를 굳이 두는 이유는, 빈 곳을 눌러 닫는 길이 있어도 그 길이 화면에 안 보이기
 * 때문이다. 처음 펼친 사람은 어디를 눌러야 접히는지 모른 채로 화면을 한참 들여다본다.
 *
 * 부모가 `pointer-events-none` 이라 여기서 되살려 준다. 되살리지 않으면 두 버튼도
 * 뒤로 클릭이 넘어가서 편집하기가 안 눌린다.
 */
function FanActions({ onEdit, onClose }: { onEdit: () => void; onClose: () => void }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ ...springSnap, delay: 0.1 }}
      className="pointer-events-auto mt-4 flex items-center justify-center gap-2"
    >
      <motion.button
        type="button"
        onClick={onEdit}
        whileTap={{ scale: 0.95 }}
        className="rounded-full bg-ink px-4 py-2 text-[12px] font-bold text-white"
      >
        편집하기
      </motion.button>
      <motion.button
        type="button"
        onClick={onClose}
        whileTap={{ scale: 0.95 }}
        className="rounded-full border border-neutral-200 bg-white px-4 py-2 text-[12px] font-bold text-neutral-500"
      >
        닫기
      </motion.button>
    </motion.div>
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
  notification: { id: string; kind: string; title: string }
  onSelect: (id: string, kind: string) => void
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
          onSelect(notification.id, notification.kind)
        }}
        whileTap={{ scale: 0.98 }}
        transition={springSnap}
        className="relative flex w-full cursor-grab touch-pan-y items-center gap-3 rounded-2xl bg-neutral-50 p-3.5 text-left active:cursor-grabbing"
      >
        <span aria-hidden className="size-9 shrink-0 rounded-xl bg-alarm" />
        <span className="min-w-0 flex-1">
          <span className="block text-[14px] font-bold text-ink">{notification.title}</span>
          {/*
            부제는 종류와 무관하게 고정이다. 시안 정리판에 "알림 variation은 메인 텍스트만
            변경" 이라고 적혀 있고 패널 시안(225:27684)의 다섯 줄도 전부 같다. 서버가 주는
            body 는 화면이 없는 잠금화면 푸시용이라 여기서 쓰지 않는다.
          */}
          <span className="block text-[12px] text-neutral-400">탭하여 확인</span>
        </span>
        <span className="text-[18px] text-neutral-300">›</span>
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
  pushState,
  onEnablePush,
}: {
  open: boolean
  onClose: () => void
  notifications: { id: string; kind: string; title: string }[]
  onSelect: (id: string, kind: string) => void
  onDismiss: (id: string) => void
  pushState: PushState
  onEnablePush: () => void
}) {
  const list =
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

  // 모바일 시트와 데스크톱 판이 이 하나를 같이 쓴다.
  const body = (
    <>
      <PushOptInBanner state={pushState} onEnable={onEnablePush} />
      {list}
    </>
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
